package com.kixyu9527.fcmcommon.xposed.legacy;

import android.content.Intent;
import android.os.SystemClock;

import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedBridge;
import com.kixyu9527.fcmcommon.xposed.compat.XposedHelpers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class GmsReconnectFix extends LegacyHookModule {
    private static final String HEARTBEAT_ALARM = "GCM_HB_ALARM";
    private static final String RECONNECT_ALARM = "GCM_CONN_ALARM";
    private static final String ACTION_GCM_RECONNECT = "com.google.android.intent.action.GCM_RECONNECT";
    private static final String GMS_PACKAGE_NAME = "com.google.android.gms";

    private final Map<String, Timer> pendingChecks = new ConcurrentHashMap<>();
    private final List<Field> timerFields = new ArrayList<>();
    private Method timeoutMethod;

    public GmsReconnectFix(ClassLoader classLoader) {
        super(classLoader);
        startHook();
    }

    @Override
    protected void onCanReadConfig() {
        if (!getBooleanConfig("gms_reconnect_tuning", true)) {
            cancelPendingChecks();
        }
    }

    private void startHook() {
        try {
            Class<?> timerClass = resolveTimerClass();
            timeoutMethod = resolveTimeoutMethod(timerClass);
            timerFields.addAll(collectFields(timerClass));
            XposedBridge.hookMethod(timeoutMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!getBooleanConfig("gms_reconnect_tuning", true)) {
                        return;
                    }
                    String alarmType = resolveAlarmType(param.thisObject);
                    if (!isTargetAlarm(alarmType) || !(param.args[0] instanceof Long delayMillis)) {
                        return;
                    }
                    if (delayMillis < 0L) {
                        param.args[0] = 0L;
                        printLog("修正负数 GCM 定时器: " + alarmType);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!getBooleanConfig("gms_reconnect_tuning", true)) {
                        return;
                    }
                    String alarmType = resolveAlarmType(param.thisObject);
                    if (!isTargetAlarm(alarmType) || !(param.args[0] instanceof Long delayMillis)) {
                        return;
                    }
                    scheduleReconnectCheck(param.thisObject, alarmType, Math.max(delayMillis, 0L));
                }
            });
            printLog("GMS 重连定时器已接管");
        } catch (Throwable throwable) {
            printLog("GMS 重连调优初始化失败: " + throwable.getMessage());
        }
    }

    private Class<?> resolveTimerClass() {
        Class<?> heartbeatAlarmClass = XposedHelpers.findClass(
            "com.google.android.gms.gcm.connection.HeartbeatChimeraAlarm",
            classLoader
        );
        for (Constructor<?> constructor : heartbeatAlarmClass.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length > 3) {
                Class<?> timerClass = parameterTypes[3];
                if (timerClass.getDeclaredMethods().length == 0 && timerClass.getSuperclass() != null) {
                    timerClass = timerClass.getSuperclass();
                }
                return timerClass;
            }
        }
        throw new IllegalStateException("未找到 GCM timer class");
    }

    private Method resolveTimeoutMethod(Class<?> timerClass) {
        Method fallback = null;
        for (Method method : timerClass.getDeclaredMethods()) {
            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == long.class) {
                method.setAccessible(true);
                if (Modifier.isFinal(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                    return method;
                }
                fallback = method;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new NoSuchMethodError(timerClass.getName() + "#setTimeout");
    }

    private void scheduleReconnectCheck(Object timerObject, String alarmType, long delayMillis) {
        String taskKey = System.identityHashCode(timerObject) + ":" + alarmType;
        Timer previous = pendingChecks.remove(taskKey);
        if (previous != null) {
            previous.cancel();
        }

        Timer timer = new Timer("FcmCommonReconnectCheck", true);
        pendingChecks.put(taskKey, timer);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (!getBooleanConfig("gms_reconnect_tuning", true)) {
                        return;
                    }
                    long nextConnectionTime = resolveNextConnectionTime(timerObject);
                    if (nextConnectionTime != 0L
                        && nextConnectionTime - SystemClock.elapsedRealtime() < -60_000L) {
                        triggerReconnect(alarmType);
                    }
                } catch (Throwable throwable) {
                    printLog("GMS 重连检查失败: " + throwable.getMessage());
                } finally {
                    pendingChecks.remove(taskKey);
                    timer.cancel();
                }
            }
        }, delayMillis + 5_000L);
    }

    private long resolveNextConnectionTime(Object timerObject) throws IllegalAccessException {
        Field bestField = null;
        long bestValue = Long.MIN_VALUE;
        for (Field field : timerFields) {
            if (field.getType() != long.class) {
                continue;
            }
            long value = field.getLong(timerObject);
            if (bestField == null || value > bestValue) {
                bestField = field;
                bestValue = value;
            }
        }
        return bestField != null ? bestField.getLong(timerObject) : 0L;
    }

    private void triggerReconnect(String alarmType) {
        if (context == null) {
            return;
        }
        Intent reconnectIntent = new Intent(ACTION_GCM_RECONNECT);
        reconnectIntent.setPackage(GMS_PACKAGE_NAME);
        context.sendBroadcast(reconnectIntent);
        printLog("触发 GCM 重连: " + alarmType, true);
    }

    private String resolveAlarmType(Object timerObject) {
        for (Field field : timerFields) {
            if (field.getType().isPrimitive() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                Object nested = field.get(timerObject);
                String alarmType = resolveAlarmTypeFromObject(nested);
                if (isTargetAlarm(alarmType)) {
                    return alarmType;
                }
            } catch (Throwable ignored) {
            }
        }
        return resolveAlarmTypeFromObject(timerObject);
    }

    private String resolveAlarmTypeFromObject(Object object) {
        if (object == null) {
            return null;
        }
        for (Field field : collectFields(object.getClass())) {
            if (field.getType() != String.class || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                Object value = field.get(object);
                if (value instanceof String alarmType && isTargetAlarm(alarmType)) {
                    return alarmType;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private List<Field> collectFields(Class<?> clazz) {
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private boolean isTargetAlarm(String alarmType) {
        return HEARTBEAT_ALARM.equals(alarmType) || RECONNECT_ALARM.equals(alarmType);
    }

    private void cancelPendingChecks() {
        for (Timer timer : pendingChecks.values()) {
            timer.cancel();
        }
        pendingChecks.clear();
    }
}
