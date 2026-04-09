package com.kixyu9527.fcmcommon.xposed.legacy;

import android.content.Context;

import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedHelpers;
import com.kixyu9527.fcmcommon.xposed.compat.XposedUtils;

import java.lang.reflect.Field;
import java.util.List;

public class PowerkeeperFix extends LegacyHookModule {

    public PowerkeeperFix(ClassLoader classLoader) {
        super(classLoader);
        startHook();
    }

    protected void startHook() {
        try {
            Class<?> milletConfig = XposedHelpers.findClassIfExists(
                "com.miui.powerkeeper.millet.MilletConfig",
                classLoader
            );
            if (milletConfig != null) {
                XposedHelpers.setStaticBooleanField(milletConfig, "isGlobal", true);
            }

            Class<?> misc = XposedHelpers.findClassIfExists(
                "com.miui.powerkeeper.provider.SimpleSettings.Misc",
                classLoader
            );
            if (misc != null) {
                XposedUtils.findAndHookMethod(misc, "getBoolean", 3, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!getBooleanConfig("powerkeeper_bypass", true)) {
                            return;
                        }
                        if ("gms_control".equals(param.args[1])) {
                            printLog("PowerKeeper GMS Limitation bypassed.", true);
                            param.setResult(false);
                        }
                    }
                });
            }

            Class<?> milletPolicy = XposedHelpers.findClassIfExists(
                "com.miui.powerkeeper.millet.MilletPolicy",
                classLoader
            );
            if (milletPolicy == null) {
                return;
            }

            XposedHelpers.findAndHookConstructor(milletPolicy, Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!getBooleanConfig("powerkeeper_bypass", true)) {
                        return;
                    }

                    boolean hasSystemBlackList = false;
                    boolean hasWhiteApps = false;
                    boolean hasDataWhiteList = false;
                    for (Field field : milletPolicy.getDeclaredFields()) {
                        if ("mSystemBlackList".equals(field.getName())) {
                            hasSystemBlackList = true;
                        } else if ("whiteApps".equals(field.getName())) {
                            hasWhiteApps = true;
                        } else if ("mDataWhiteList".equals(field.getName())) {
                            hasDataWhiteList = true;
                        }
                    }

                    if (hasSystemBlackList) {
                        List<?> blackList = (List<?>) XposedHelpers.getObjectField(param.thisObject, "mSystemBlackList");
                        if (blackList != null) {
                            blackList.remove("com.google.android.gms");
                        }
                    }

                    if (hasWhiteApps) {
                        List<?> whiteApps = (List<?>) XposedHelpers.getObjectField(param.thisObject, "whiteApps");
                        if (whiteApps != null) {
                            whiteApps.remove("com.google.android.gms");
                            whiteApps.remove("com.google.android.ext.services");
                        }
                    }

                    if (hasDataWhiteList) {
                        List<String> dataWhiteList = (List<String>) XposedHelpers.getObjectField(param.thisObject, "mDataWhiteList");
                        if (dataWhiteList != null && !dataWhiteList.contains("com.google.android.gms")) {
                            dataWhiteList.add("com.google.android.gms");
                        }
                    }
                }
            });
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError error) {
            printLog("No Such PowerKeeper hook target: " + error.getMessage());
        }
    }
}
