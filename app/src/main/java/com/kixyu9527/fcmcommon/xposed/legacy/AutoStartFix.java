package com.kixyu9527.fcmcommon.xposed.legacy;

import android.content.Intent;

import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedHelpers;
import com.kixyu9527.fcmcommon.xposed.compat.XposedUtils;

public class AutoStartFix extends LegacyHookModule {

    public AutoStartFix(ClassLoader classLoader) {
        super(classLoader);
        try {
            startHook();
            startHookRemovePowerPolicy();
        } catch (Throwable throwable) {
            printLog("hook error AutoStartFix: " + throwable.getMessage());
        }
    }

    protected void startHook() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.android.server.am.BroadcastQueueInjector", classLoader);
            XposedUtils.findAndHookMethodAnyParam(clazz, "checkApplicationAutoStart", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) XposedHelpers.getObjectField(param.args[2], "intent");
                    allowBroadcastAutoStart(param, intent, false);
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError error) {
            printLog("No Such Method com.android.server.am.BroadcastQueueInjector.checkApplicationAutoStart");
        }

        try {
            Class<?> clazz = XposedHelpers.findClass("com.android.server.am.BroadcastQueueImpl", classLoader);
            XposedUtils.findAndHookMethodAnyParam(clazz, "checkApplicationAutoStart", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) XposedHelpers.getObjectField(param.args[1], "intent");
                    String target = resolveTargetPackage(intent);
                    if (shouldAllowTarget(target, intent, true)) {
                        XposedHelpers.callMethod(param.thisObject, "checkAbnormalBroadcastInQueueLocked", param.args[0]);
                        printLog("Allow Auto Start: " + target, true);
                        param.setResult(true);
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError error) {
            printLog("No Such Method com.android.server.am.BroadcastQueueImpl.checkApplicationAutoStart");
        }

        try {
            Class<?> clazz = XposedHelpers.findClass("com.android.server.am.BroadcastQueueModernStubImpl", classLoader);
            XposedUtils.findAndHookMethodAnyParam(clazz, "checkApplicationAutoStart", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) XposedHelpers.getObjectField(param.args[1], "intent");
                    String target = resolveTargetPackage(intent);
                    if (shouldAllowTarget(target, intent, false)) {
                        printLog("[" + intent.getAction() + "] checkApplicationAutoStart: " + target, true);
                        param.setResult(true);
                    }
                }
            });

            XposedUtils.findAndHookMethodAnyParam(clazz, "checkReceiverIfRestricted", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) XposedHelpers.getObjectField(param.args[1], "intent");
                    String target = resolveTargetPackage(intent);
                    if (shouldAllowTarget(target, intent, true)) {
                        printLog("checkReceiverIfRestricted: " + target, true);
                        param.setResult(false);
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError error) {
            printLog("No Such class com.android.server.am.BroadcastQueueModernStubImpl");
        }

        try {
            Class<?> clazz = XposedHelpers.findClass("com.android.server.am.AutoStartManagerServiceStubImpl", classLoader);
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) param.args[1];
                    String target = resolveTargetPackage(intent);
                    if (shouldAllowTarget(target, intent, false)) {
                        printLog("[" + intent.getAction() + "] isAllowStartService: " + target, true);
                        param.setResult(true);
                    }
                }
            };
            XC_MethodHook.Unhook first = XposedUtils.tryFindAndHookMethod(clazz, "isAllowStartService", 3, hook);
            XC_MethodHook.Unhook second = XposedUtils.tryFindAndHookMethod(clazz, "isAllowStartService", 4, hook);
            if (first == null && second == null) {
                throw new NoSuchMethodError();
            }
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError error) {
            printLog("No Such Class com.android.server.am.AutoStartManagerServiceStubImpl.isAllowStartService");
        }

        try {
            Class<?> clazz = XposedHelpers.findClass("com.android.server.am.SmartPowerService", classLoader);
            XposedUtils.findAndHookMethodAnyParam(clazz, "shouldInterceptBroadcast", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) XposedHelpers.getObjectField(param.args[1], "intent");
                    String target = resolveTargetPackage(intent);
                    if (shouldAllowTarget(target, intent, true)) {
                        printLog("SmartPowerService.shouldInterceptBroadcast: " + target, true);
                        param.setResult(false);
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError error) {
            printLog("No Such Class com.android.server.am.SmartPowerService");
        }
    }

    protected void startHookRemovePowerPolicy() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.miui.server.smartpower.SmartPowerPolicyManager", classLoader);
            XposedUtils.findAndHookMethodAnyParam(clazz, "shouldInterceptService", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Intent intent = (Intent) param.args[0];
                    String target = resolveTargetPackage(intent);
                    if ("com.google.firebase.MESSAGING_EVENT".equals(intent.getAction())
                        && shouldAllowTarget(target, intent, true)) {
                        printLog("Disable MIUI Intercept: " + target, true);
                        param.setResult(false);
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError error) {
            printLog("No Such Method com.miui.server.smartpower.SmartPowerPolicyManager.shouldInterceptService");
        }
    }

    private void allowBroadcastAutoStart(XC_MethodHook.MethodHookParam param, Intent intent, boolean fcmOnly) {
        String target = resolveTargetPackage(intent);
        if (shouldAllowTarget(target, intent, fcmOnly)) {
            Class<?> injector = XposedHelpers.findClass("com.android.server.am.BroadcastQueueInjector", classLoader);
            XposedHelpers.callStaticMethod(injector, "checkAbnormalBroadcastInQueueLocked", param.args[1], param.args[0]);
            printLog("Allow Auto Start: " + target, true);
            param.setResult(true);
        }
    }

    private boolean shouldAllowTarget(String target, Intent intent, boolean fcmOnly) {
        if (intent == null || target == null) {
            return false;
        }
        boolean allowAnyIntent = !fcmOnly && getBooleanConfig("hyperos_broadcast_shield", true);
        boolean allowFcm = isFCMIntent(intent);
        return targetIsAllow(target) && (allowAnyIntent || allowFcm);
    }
}
