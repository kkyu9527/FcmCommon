package com.kixyu9527.fcmcommon.xposed.legacy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;

import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedBridge;
import com.kixyu9527.fcmcommon.xposed.compat.XposedHelpers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class HyperOsSystemFix extends LegacyHookModule {
    private static final List<String> CN_DEFER_BROADCAST = Arrays.asList(
        "com.google.android.intent.action.GCM_RECONNECT",
        "com.google.android.gcm.DISCONNECTED",
        "com.google.android.gcm.CONNECTED",
        "com.google.android.gms.gcm.HEARTBEAT_ALARM"
    );
    private static final String ACTION_REMOTE_INTENT = "com.google.android.c2dm.intent.RECEIVE";
    private static final String ACTION_GCM_CONNECTED = "com.google.android.gcm.CONNECTED";
    private static final String ACTION_GCM_DISCONNECTED = "com.google.android.gcm.DISCONNECTED";
    private static final String GMS_PACKAGE_NAME = "com.google.android.gms";
    private static final String GMS_PERSISTENT_PROCESS_NAME = "com.google.android.gms.persistent";

    public HyperOsSystemFix(ClassLoader classLoader) {
        super(classLoader);
        startHook();
    }

    private void startHook() {
        hookGreezeManagerService();
        hookDomesticPolicyManager();
        hookListAppsManager();
        hookProcessPolicy();
        hookAwareResourceControl();
        hookActivityManagerService();
    }

    private void hookGreezeManagerService() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.miui.server.greeze.GreezeManagerService", classLoader);

            try {
                Method isAllowBroadcast = clazz.getDeclaredMethod(
                    "isAllowBroadcast",
                    int.class,
                    String.class,
                    int.class,
                    String.class,
                    String.class
                );
                Method getPackageNameFromUid = clazz.getDeclaredMethod("getPackageNameFromUid", int.class);
                getPackageNameFromUid.setAccessible(true);
                XposedBridge.hookMethod(isAllowBroadcast, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!getBooleanConfig("hyperos_broadcast_shield", true)) {
                            return;
                        }

                        String calleePkgName = param.args[3] instanceof String ? (String) param.args[3] : null;
                        try {
                            if (param.args[2] instanceof Integer calleeUid) {
                                Object result = getPackageNameFromUid.invoke(param.thisObject, calleeUid);
                                if (result instanceof String) {
                                    calleePkgName = (String) result;
                                }
                            }
                        } catch (Throwable throwable) {
                            printLog("获取 Greeze callee package 失败: " + throwable.getMessage());
                        }

                        if (!(param.args[4] instanceof String action)) {
                            return;
                        }

                        boolean allowRemoteIntent = (param.args[1] instanceof String callerPackage)
                            && GMS_PACKAGE_NAME.equals(callerPackage)
                            && ACTION_REMOTE_INTENT.equals(action);
                        boolean allowReconnectAction = (GMS_PACKAGE_NAME.equals(calleePkgName)
                            || GMS_PERSISTENT_PROCESS_NAME.equals(calleePkgName))
                            && CN_DEFER_BROADCAST.contains(action);

                        if (allowRemoteIntent || allowReconnectAction) {
                            param.setResult(true);
                        }
                    }
                });
            } catch (NoSuchMethodException error) {
                printLog("No Such Method GreezeManagerService.isAllowBroadcast");
            }

            try {
                Method deferBroadcastForMiui = clazz.getDeclaredMethod("deferBroadcastForMiui", String.class);
                XposedBridge.hookMethod(deferBroadcastForMiui, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!getBooleanConfig("gms_reconnect_tuning", true)) {
                            return;
                        }
                        if (param.args[0] instanceof String action && CN_DEFER_BROADCAST.contains(action)) {
                            param.setResult(false);
                        }
                    }
                });
            } catch (NoSuchMethodException error) {
                printLog("No Such Method GreezeManagerService.deferBroadcastForMiui");
            }

            try {
                Method triggerGmsLimitAction = clazz.getDeclaredMethod("triggerGMSLimitAction", boolean.class);
                XposedBridge.hookMethod(triggerGmsLimitAction, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!getBooleanConfig("powerkeeper_bypass", true)) {
                            return;
                        }
                        param.args[0] = false;
                    }
                });
            } catch (NoSuchMethodException error) {
                printLog("No Such Method GreezeManagerService.triggerGMSLimitAction");
            }
        } catch (XposedHelpers.ClassNotFoundError error) {
            printLog("Not found com.miui.server.greeze.GreezeManagerService");
        }
    }

    private void hookDomesticPolicyManager() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.miui.server.greeze.DomesticPolicyManager", classLoader);
            Method deferBroadcast = clazz.getDeclaredMethod("deferBroadcast", String.class);
            XposedBridge.hookMethod(deferBroadcast, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!getBooleanConfig("gms_reconnect_tuning", true)) {
                        return;
                    }
                    if (param.args[0] instanceof String action
                        && (ACTION_REMOTE_INTENT.equals(action) || CN_DEFER_BROADCAST.contains(action))) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable throwable) {
            printLog("Failed to hook DomesticPolicyManager: " + throwable.getMessage());
        }
    }

    private void hookListAppsManager() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.miui.server.greeze.power.ListAppsManager", classLoader);
            Field blackListField = clazz.getDeclaredField("mSystemBlackList");
            blackListField.setAccessible(true);
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!getBooleanConfig("powerkeeper_bypass", true)) {
                            return;
                        }
                        try {
                            List<String> blackList = (List<String>) blackListField.get(param.thisObject);
                            if (blackList != null) {
                                blackList.remove(GMS_PACKAGE_NAME);
                            }
                        } catch (Throwable throwable) {
                            printLog("修改 ListAppsManager.mSystemBlackList 失败: " + throwable.getMessage());
                        }
                    }
                });
            }
        } catch (Throwable throwable) {
            printLog("Failed to hook ListAppsManager: " + throwable.getMessage());
        }
    }

    private void hookProcessPolicy() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.android.server.am.ProcessPolicy", classLoader);
            Method getWhiteList = clazz.getDeclaredMethod("getWhiteList", int.class);
            XposedBridge.hookMethod(getWhiteList, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!getBooleanConfig("gms_reconnect_tuning", true)) {
                        return;
                    }
                    if (!(param.args[0] instanceof Integer flags) || (flags & 1) == 0) {
                        return;
                    }
                    if (param.getResult() instanceof List<?> list) {
                        List<String> whiteList = (List<String>) list;
                        if (!whiteList.contains(GMS_PACKAGE_NAME)) {
                            whiteList.add(GMS_PACKAGE_NAME);
                        }
                        if (!whiteList.contains(GMS_PERSISTENT_PROCESS_NAME)) {
                            whiteList.add(GMS_PERSISTENT_PROCESS_NAME);
                        }
                    }
                }
            });
        } catch (Throwable throwable) {
            printLog("Failed to hook ProcessPolicy: " + throwable.getMessage());
        }
    }

    private void hookAwareResourceControl() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.miui.server.greeze.power.AwareResourceControl", classLoader);
            Field noNetworkBlackUids = clazz.getDeclaredField("mNoNetworkBlackUids");
            noNetworkBlackUids.setAccessible(true);
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!getBooleanConfig("powerkeeper_bypass", true)) {
                            return;
                        }
                        try {
                            List<String> blocked = (List<String>) noNetworkBlackUids.get(param.thisObject);
                            if (blocked != null) {
                                blocked.remove(GMS_PACKAGE_NAME);
                            }
                        } catch (Throwable throwable) {
                            printLog("修改 AwareResourceControl.mNoNetworkBlackUids 失败: " + throwable.getMessage());
                        }
                    }
                });
            }
        } catch (Throwable throwable) {
            printLog("Failed to hook AwareResourceControl: " + throwable.getMessage());
        }
    }

    private void hookActivityManagerService() {
        try {
            Class<?> activityManagerService = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", classLoader);
            Field contextField = activityManagerService.getDeclaredField("mContext");
            contextField.setAccessible(true);
            Class<?> applicationThread = XposedHelpers.findClass("android.app.IApplicationThread", classLoader);
            Class<?> intentReceiver = XposedHelpers.findClass("android.content.IIntentReceiver", classLoader);
            Class<?> processRecord = XposedHelpers.findClass("com.android.server.am.ProcessRecord", classLoader);
            Field infoField = processRecord.getDeclaredField("info");
            infoField.setAccessible(true);

            Method getRecordMethod;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getRecordMethod = activityManagerService.getDeclaredMethod("getRecordForAppLOSP", applicationThread);
            } else {
                getRecordMethod = activityManagerService.getDeclaredMethod("getRecordForAppLocked", applicationThread);
            }
            getRecordMethod.setAccessible(true);

            Method broadcastMethod;
            int intentArgIndex;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intentArgIndex = 2;
                broadcastMethod = activityManagerService.getDeclaredMethod(
                    "broadcastIntentWithFeature",
                    applicationThread,
                    String.class,
                    Intent.class,
                    String.class,
                    intentReceiver,
                    int.class,
                    String.class,
                    Bundle.class,
                    String[].class,
                    String[].class,
                    String[].class,
                    int.class,
                    Bundle.class,
                    boolean.class,
                    boolean.class,
                    int.class
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intentArgIndex = 2;
                broadcastMethod = activityManagerService.getDeclaredMethod(
                    "broadcastIntentWithFeature",
                    applicationThread,
                    String.class,
                    Intent.class,
                    String.class,
                    intentReceiver,
                    int.class,
                    String.class,
                    Bundle.class,
                    String[].class,
                    String[].class,
                    int.class,
                    Bundle.class,
                    boolean.class,
                    boolean.class,
                    int.class
                );
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                intentArgIndex = 2;
                broadcastMethod = activityManagerService.getDeclaredMethod(
                    "broadcastIntentWithFeature",
                    applicationThread,
                    String.class,
                    Intent.class,
                    String.class,
                    intentReceiver,
                    int.class,
                    String.class,
                    Bundle.class,
                    String[].class,
                    int.class,
                    Bundle.class,
                    boolean.class,
                    boolean.class,
                    int.class
                );
            } else {
                intentArgIndex = 1;
                broadcastMethod = activityManagerService.getDeclaredMethod(
                    "broadcastIntent",
                    applicationThread,
                    Intent.class,
                    String.class,
                    intentReceiver,
                    int.class,
                    String.class,
                    Bundle.class,
                    String[].class,
                    int.class,
                    Bundle.class,
                    boolean.class,
                    boolean.class,
                    int.class
                );
            }

            final int finalIntentArgIndex = intentArgIndex;
            XposedBridge.hookMethod(broadcastMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!(param.args[finalIntentArgIndex] instanceof Intent intent)) {
                        return;
                    }
                    String action = intent.getAction();
                    if (!ACTION_REMOTE_INTENT.equals(action)
                        && !ACTION_GCM_CONNECTED.equals(action)
                        && !ACTION_GCM_DISCONNECTED.equals(action)) {
                        return;
                    }

                    try {
                        Object callerRecord = getRecordMethod.invoke(param.thisObject, param.args[0]);
                        if (callerRecord == null) {
                            return;
                        }
                        Object appInfo = infoField.get(callerRecord);
                        if (!(appInfo instanceof ApplicationInfo info) || !GMS_PACKAGE_NAME.equals(info.packageName)) {
                            return;
                        }
                    } catch (Throwable throwable) {
                        printLog("识别 GMS 调用者失败: " + throwable.getMessage());
                        return;
                    }

                    if (ACTION_GCM_CONNECTED.equals(action)) {
                        recordFcmDiagnosticsState(true, "Google Play 服务发出了 FCM 已连接广播。");
                        return;
                    }

                    if (ACTION_GCM_DISCONNECTED.equals(action)) {
                        recordFcmDiagnosticsState(
                            false,
                            buildDisconnectDetail(intent.getExtras())
                        );
                        return;
                    }

                    String targetPackage = resolveTargetPackage(intent);
                    if (targetPackage == null) {
                        return;
                    }

                    if (getBooleanConfig("powerkeeper_bypass", true)) {
                        tryAddTemporaryAllowList(contextField, param.thisObject, targetPackage);
                    }

                    if (getBooleanConfig("wake_stopped_apps", true)
                        && targetIsAllow(targetPackage)
                        && (intent.getFlags() & Intent.FLAG_INCLUDE_STOPPED_PACKAGES) == 0) {
                        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    }
                }
            });
        } catch (Throwable throwable) {
            printLog("Failed to hook ActivityManagerService: " + throwable.getMessage());
        }
    }

    private String buildDisconnectDetail(Bundle extras) {
        if (extras == null || extras.isEmpty()) {
            return "Google Play 服务发出了 FCM 已断开广播。";
        }
        for (String key : Arrays.asList("reason", "error", "message")) {
            Object value = extras.get(key);
            if (value != null) {
                return "Google Play 服务断连: " + value;
            }
        }
        return "Google Play 服务发出了 FCM 已断开广播。";
    }

    private void tryAddTemporaryAllowList(Field contextField, Object activityManagerService, String packageName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        try {
            Object systemContext = contextField.get(activityManagerService);
            if (!(systemContext instanceof Context context)) {
                return;
            }

            Object manager = context.getSystemService("power_exemption");
            if (manager == null) {
                Class<?> clazz = Class.forName("android.os.PowerExemptionManager");
                Constructor<?> constructor = clazz.getDeclaredConstructor(Context.class);
                constructor.setAccessible(true);
                manager = constructor.newInstance(context);
            }
            Method method = manager.getClass().getDeclaredMethod(
                "addToTemporaryAllowList",
                String.class,
                int.class,
                String.class,
                long.class
            );
            method.setAccessible(true);
            method.invoke(manager, packageName, 102, "GOOGLE_C2DM", 2000L);
        } catch (Throwable throwable) {
            printLog("临时电量白名单添加失败: " + throwable.getMessage());
        }
    }
}
