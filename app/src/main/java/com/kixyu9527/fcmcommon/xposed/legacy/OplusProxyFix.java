package com.kixyu9527.fcmcommon.xposed.legacy;

import android.content.pm.PackageManager;
import android.os.WorkSource;

import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedHelpers;
import com.kixyu9527.fcmcommon.xposed.compat.XposedUtils;

@SuppressWarnings("unchecked")
public class OplusProxyFix extends LegacyHookModule {
    private static Object proxyWakeLock;
    private static volatile boolean useFourParamUnfreeze;
    private static volatile boolean hasDetectedSignature;

    public OplusProxyFix(ClassLoader classLoader) {
        super(classLoader);
        tryHookBroadcastProxy();
        tryHookWakeLockProxy();
        tryHookHansRestrictions();
    }

    private void tryHookBroadcastProxy() {
        try {
            Class<?> proxyBroadcastClass = XposedHelpers.findClass(
                "com.android.server.am.OplusProxyBroadcast",
                classLoader
            );
            Class<?> resultEnum = XposedHelpers.findClass(
                "com.android.server.am.OplusProxyBroadcast$RESULT",
                classLoader
            );
            Object notInclude = Enum.valueOf((Class<? extends Enum>) resultEnum.asSubclass(Enum.class), "NOT_INCLUDE");

            XposedUtils.tryFindAndHookMethod(proxyBroadcastClass, "shouldProxy", 8, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!getBooleanConfig("hyperos_broadcast_shield", true)) {
                        return;
                    }
                    String packageName = param.args[5] instanceof String ? (String) param.args[5] : null;
                    String action = param.args[6] instanceof String ? (String) param.args[6] : null;
                    if (isFCMAction(action) && targetIsAllow(packageName)) {
                        param.setResult(notInclude);
                    }
                }
            });
        } catch (Throwable throwable) {
            printLog("OplusProxyBroadcast 未启用: " + throwable.getMessage());
        }
    }

    private void tryHookWakeLockProxy() {
        try {
            Class<?> wakeLockClass = XposedHelpers.findClass(
                "com.android.server.power.OplusProxyWakeLock",
                classLoader
            );
            XposedUtils.findAndHookConstructorAnyParam(wakeLockClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    proxyWakeLock = param.thisObject;
                }
            });
        } catch (Throwable throwable) {
            printLog("OplusProxyWakeLock 未启用: " + throwable.getMessage());
        }
    }

    private void tryHookHansRestrictions() {
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.server.hans.scene.OplusBgSceneManager",
                classLoader,
                "registerGmsRestrictObserver",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (getBooleanConfig("powerkeeper_bypass", true)) {
                            param.setResult(null);
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(
                "com.android.server.hans.scene.OplusBgSceneManager",
                classLoader,
                "updateGmsRestrict",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (getBooleanConfig("powerkeeper_bypass", true)) {
                            param.setResult(null);
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(
                "com.android.server.am.OplusAppStartupManager$OplusStartupStrategy",
                classLoader,
                "isGoogleRestricInfoOn",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (getBooleanConfig("powerkeeper_bypass", true)) {
                            param.setResult(false);
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }
    }

    public static void unfreeze(String packageName) {
        if (proxyWakeLock == null || context == null || packageName == null) {
            return;
        }
        int uid = resolvePackageUid(packageName);
        if (uid < 0) {
            return;
        }

        WorkSource workSource = new WorkSource();
        String tag = "FcmCommon";
        try {
            if (!hasDetectedSignature) {
                try {
                    XposedHelpers.callMethod(proxyWakeLock, "unfreezeIfNeed", uid, workSource, tag, "FcmCommon");
                    useFourParamUnfreeze = true;
                } catch (Throwable ignored) {
                    XposedHelpers.callMethod(proxyWakeLock, "unfreezeIfNeed", uid, workSource, tag);
                    useFourParamUnfreeze = false;
                }
                hasDetectedSignature = true;
            } else if (useFourParamUnfreeze) {
                XposedHelpers.callMethod(proxyWakeLock, "unfreezeIfNeed", uid, workSource, tag, "FcmCommon");
            } else {
                XposedHelpers.callMethod(proxyWakeLock, "unfreezeIfNeed", uid, workSource, tag);
            }
        } catch (Throwable throwable) {
            printLog("Oplus unfreeze 失败: " + throwable.getMessage());
        }
    }

    private static int resolvePackageUid(String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getPackageUid(packageName, 0);
        } catch (PackageManager.NameNotFoundException throwable) {
            return -1;
        }
    }
}
