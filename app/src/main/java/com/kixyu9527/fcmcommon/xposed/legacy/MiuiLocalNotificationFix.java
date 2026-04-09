package com.kixyu9527.fcmcommon.xposed.legacy;

import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedBridge;
import com.kixyu9527.fcmcommon.xposed.compat.XposedHelpers;

import java.lang.reflect.Method;

public class MiuiLocalNotificationFix extends LegacyHookModule {

    public MiuiLocalNotificationFix(ClassLoader classLoader) {
        super(classLoader);
        startHook();
    }

    protected void startHook() {
        try {
            Class<?> clazz;
            try {
                clazz = XposedHelpers.findClass("com.android.server.notification.NotificationManagerServiceInjector", classLoader);
            } catch (XposedHelpers.ClassNotFoundError error) {
                clazz = XposedHelpers.findClass("com.android.server.notification.NotificationManagerServiceImpl", classLoader);
            }
            Method targetMethod = null;
            for (Method method : clazz.getDeclaredMethods()) {
                if ("isAllowLocalNotification".equals(method.getName()) || "isDeniedLocalNotification".equals(method.getName())) {
                    targetMethod = method;
                    break;
                }
            }
            if (targetMethod == null) {
                printLog("Not found local notification method");
                return;
            }
            Method finalTargetMethod = targetMethod;
            XposedBridge.hookMethod(targetMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!getBooleanConfig("local_notification_bypass", true)) {
                        return;
                    }
                    if (targetIsAllow((String) param.args[3])) {
                        param.setResult("isAllowLocalNotification".equals(finalTargetMethod.getName()));
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError error) {
            printLog("Not found local notification class");
        }
    }
}
