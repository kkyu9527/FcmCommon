package com.kixyu9527.fcmcommon.xposed.legacy;

import android.os.Build;
import android.service.notification.NotificationListenerService;

import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedBridge;
import com.kixyu9527.fcmcommon.xposed.compat.XposedHelpers;

import java.lang.reflect.Method;

public class KeepNotification extends LegacyHookModule {

    public KeepNotification(ClassLoader classLoader) {
        super(classLoader);
        try {
            startHook();
        } catch (Throwable throwable) {
            printLog("No Such Method com.android.server.notification.NotificationManagerService.cancelAllNotificationsInt");
        }
    }

    protected void startHook() {
        Class<?> clazz = XposedHelpers.findClass("com.android.server.notification.NotificationManagerService", classLoader);
        Method targetMethod = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if ("cancelAllNotificationsInt".equals(method.getName())) {
                if (targetMethod == null || targetMethod.getParameterTypes().length < method.getParameterTypes().length) {
                    targetMethod = method;
                }
            }
        }
        if (targetMethod == null) {
            throw new NoSuchMethodError();
        }

        int pkgArgIndex = 0;
        int reasonArgIndex = 0;
        if (Build.VERSION.SDK_INT >= 30 && Build.VERSION.SDK_INT <= 33) {
            pkgArgIndex = 2;
            reasonArgIndex = 8;
        } else if (Build.VERSION.SDK_INT == 34) {
            if (targetMethod.getParameterTypes().length == 10) {
                pkgArgIndex = 2;
                reasonArgIndex = 8;
            } else if (targetMethod.getParameterTypes().length == 8) {
                pkgArgIndex = 2;
                reasonArgIndex = 7;
            }
        } else if (Build.VERSION.SDK_INT >= 35) {
            pkgArgIndex = 2;
            reasonArgIndex = 7;
        }
        if (pkgArgIndex == 0 || reasonArgIndex == 0) {
            throw new NoSuchMethodError();
        }

        final int finalPkgArgIndex = pkgArgIndex;
        final int finalReasonArgIndex = reasonArgIndex;
        XposedBridge.hookMethod(targetMethod, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!isBootComplete || !getBooleanConfig("keep_notifications", false)) {
                    return;
                }
                String packageName = (String) param.args[finalPkgArgIndex];
                if (!targetIsAllow(packageName)) {
                    return;
                }
                int reason = (int) param.args[finalReasonArgIndex];
                if (reason == NotificationListenerService.REASON_PACKAGE_CHANGED || reason == 10020 || reason == 10021) {
                    param.setResult(null);
                }
            }
        });
    }
}
