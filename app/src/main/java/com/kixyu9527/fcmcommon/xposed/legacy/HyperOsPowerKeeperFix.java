package com.kixyu9527.fcmcommon.xposed.legacy;

import android.os.Bundle;

import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedBridge;
import com.kixyu9527.fcmcommon.xposed.compat.XposedHelpers;

import java.lang.reflect.Method;
import java.util.List;

public class HyperOsPowerKeeperFix extends LegacyHookModule {
    private static final String GMS_PACKAGE_NAME = "com.google.android.gms";

    public HyperOsPowerKeeperFix(ClassLoader classLoader) {
        super(classLoader);
        startHook();
    }

    private void startHook() {
        hookGmsObserver();
        hookGlobalFeatureConfigureHelper();
    }

    private void hookGmsObserver() {
        try {
            Class<?> netdExecutor = XposedHelpers.findClass("com.miui.powerkeeper.utils.NetdExecutor", classLoader);
            Method initGmsChain = netdExecutor.getDeclaredMethod("initGmsChain", String.class, int.class, String.class);
            XposedBridge.hookMethod(initGmsChain, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!getBooleanConfig("powerkeeper_bypass", true)) {
                        return;
                    }
                    param.args[2] = "ACCEPT";
                }
            });

            Class<?> gmsObserver = XposedHelpers.findClass("com.miui.powerkeeper.utils.GmsObserver", classLoader);
            hookBooleanOverride(gmsObserver, "updateGmsAlarm");
            hookBooleanOverride(gmsObserver, "updateGmsNetWork");
            hookBooleanOverride(gmsObserver, "updateGoogleReletivesWakelock");
        } catch (Throwable throwable) {
            printLog("Failed to hook GmsObserver: " + throwable.getMessage());
        }
    }

    private void hookBooleanOverride(Class<?> clazz, String methodName) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, boolean.class);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!getBooleanConfig("powerkeeper_bypass", true)) {
                    return;
                }
                param.args[0] = false;
            }
        });
    }

    private void hookGlobalFeatureConfigureHelper() {
        try {
            Class<?> clazz = XposedHelpers.findClass(
                "com.miui.powerkeeper.provider.GlobalFeatureConfigureHelper",
                classLoader
            );
            Method getDozeWhiteListApps = clazz.getDeclaredMethod("getDozeWhiteListApps", Bundle.class);
            XposedBridge.hookMethod(getDozeWhiteListApps, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!getBooleanConfig("powerkeeper_bypass", true)) {
                        return;
                    }
                    if (param.getResult() instanceof List<?> list) {
                        List<String> whiteList = (List<String>) list;
                        if (!whiteList.contains(GMS_PACKAGE_NAME)) {
                            whiteList.add(GMS_PACKAGE_NAME);
                        }
                    }
                }
            });
        } catch (Throwable throwable) {
            printLog("Failed to hook GlobalFeatureConfigureHelper: " + throwable.getMessage());
        }
    }
}
