package com.kixyu9527.fcmcommon.xposed.legacy;

import android.content.Intent;
import android.os.Build;

import com.kixyu9527.fcmcommon.xposed.compat.IceboxUtils;
import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedBridge;
import com.kixyu9527.fcmcommon.xposed.compat.XposedUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class BroadcastFix extends LegacyHookModule {

    public BroadcastFix(ClassLoader classLoader) {
        super(classLoader);
        try {
            startHookBroadcastIntentLocked();
        } catch (Throwable throwable) {
            printLog("hook error broadcastIntentLocked: " + throwable.getMessage());
        }
    }

    protected void startHookBroadcastIntentLocked() {
        Method targetMethod = null;
        int intentArgIndex = 0;
        int appOpArgIndex = 0;

        if (Build.VERSION.SDK_INT >= 35) {
            targetMethod = XposedUtils.tryFindMethodMostParam(
                classLoader,
                "com.android.server.am.BroadcastController",
                "broadcastIntentLocked"
            );
            if (targetMethod != null) {
                intentArgIndex = 3;
                appOpArgIndex = 13;
            }
        }

        if (targetMethod == null) {
            targetMethod = XposedUtils.tryFindMethodMostParam(
                classLoader,
                "com.android.server.am.ActivityManagerService",
                "broadcastIntentLocked"
            );
            if (targetMethod != null) {
                Parameter[] parameters = targetMethod.getParameters();
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    intentArgIndex = 2;
                    appOpArgIndex = 9;
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    intentArgIndex = 3;
                    appOpArgIndex = 10;
                } else if (Build.VERSION.SDK_INT == 31 || Build.VERSION.SDK_INT == 32) {
                    intentArgIndex = 3;
                    if (parameters.length > 11 && parameters[11].getType() == int.class) {
                        appOpArgIndex = 11;
                    }
                    if (parameters.length > 12 && parameters[12].getType() == int.class) {
                        appOpArgIndex = 12;
                    }
                } else if (Build.VERSION.SDK_INT == 33) {
                    intentArgIndex = 3;
                    appOpArgIndex = 12;
                } else if (Build.VERSION.SDK_INT >= 34) {
                    intentArgIndex = 3;
                    if (parameters.length > 12 && parameters[12].getType() == int.class) {
                        appOpArgIndex = 12;
                    }
                    if (parameters.length > 13 && parameters[13].getType() == int.class) {
                        appOpArgIndex = 13;
                    }
                }

                if (intentArgIndex == 0 || appOpArgIndex == 0) {
                    intentArgIndex = 0;
                    appOpArgIndex = 0;
                    for (int i = 0; i < parameters.length; i++) {
                        if ("appOp".equals(parameters[i].getName()) && parameters[i].getType() == int.class) {
                            appOpArgIndex = i;
                        }
                        if ("intent".equals(parameters[i].getName()) && parameters[i].getType() == Intent.class) {
                            intentArgIndex = i;
                        }
                    }
                }
            }
        }

        if (targetMethod == null
            || intentArgIndex == 0
            || appOpArgIndex == 0
            || targetMethod.getParameters()[intentArgIndex].getType() != Intent.class
            || targetMethod.getParameters()[appOpArgIndex].getType() != int.class) {
            printLog("broadcastIntentLocked hook 位置查找失败，FcmCommon 将不会修复 stopped app 投递。");
            return;
        }

        createBroadcastIntentLockedHooker(intentArgIndex, appOpArgIndex, targetMethod);
    }

    protected void createBroadcastIntentLockedHooker(
        int intentArgIndex,
        int appOpArgIndex,
        Method method
    ) {
        final int finalIntentArgIndex = intentArgIndex;
        final int finalAppOpArgIndex = appOpArgIndex;
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!isBootComplete || param.args[finalIntentArgIndex] == null) {
                    return;
                }

                Intent intent = (Intent) param.args[finalIntentArgIndex];
                if (!isFCMIntent(intent)) {
                    return;
                }

                String target = resolveTargetPackage(intent);
                if (!targetIsAllow(target)) {
                    return;
                }

                boolean shieldEnabled = getBooleanConfig("hyperos_broadcast_shield", true);
                boolean wakeStoppedAppsEnabled = getBooleanConfig("wake_stopped_apps", true);
                if (!shieldEnabled && !wakeStoppedAppsEnabled) {
                    return;
                }

                if (shieldEnabled && ((Integer) param.args[finalAppOpArgIndex]) == -1) {
                    param.args[finalAppOpArgIndex] = 11;
                }

                if (wakeStoppedAppsEnabled && (intent.getFlags() & Intent.FLAG_INCLUDE_STOPPED_PACKAGES) == 0) {
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                }

                if (wakeStoppedAppsEnabled
                    && getBooleanConfig("icebox_wakeup", false)
                    && context != null
                    && !IceboxUtils.isAppEnabled(context, target)) {
                    printLog("等待 Ice Box 解冻应用: " + target, true);
                    param.setResult(false);
                    new Thread(() -> {
                        IceboxUtils.activeApp(context, target);
                        for (int i = 0; i < 300; i++) {
                            if (IceboxUtils.isAppEnabled(context, target)) {
                                break;
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                        try {
                            if (IceboxUtils.isAppEnabled(context, target)) {
                                printLog("发送解冻后的 FCM 广播: " + target, true);
                            } else {
                                printLog("等待 Ice Box 解冻超时: " + target, true);
                            }
                            XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                        } catch (Throwable throwable) {
                            printLog("发送解冻广播失败: " + target + " " + throwable.getMessage(), true);
                        }
                    }).start();
                } else if (wakeStoppedAppsEnabled) {
                    printLog("放行 stopped app 广播: " + target, true);
                }
            }
        });
    }
}
