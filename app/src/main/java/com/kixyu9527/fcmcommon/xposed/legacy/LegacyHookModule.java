package com.kixyu9527.fcmcommon.xposed.legacy;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.kixyu9527.fcmcommon.data.ConfigKeys;
import com.kixyu9527.fcmcommon.xposed.compat.XC_MethodHook;
import com.kixyu9527.fcmcommon.xposed.compat.XposedBridge;
import com.kixyu9527.fcmcommon.xposed.compat.XposedHelpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class LegacyHookModule {
    private static final String TAG = "FcmCommon";
    private static final String CHANNEL_ID = "fcmcommon";
    private static final Set<String> DEFAULT_FEATURES = Set.of(
        "hyperos_broadcast_shield",
        "wake_stopped_apps",
        "powerkeeper_bypass",
        "gms_reconnect_tuning",
        "local_notification_bypass"
    );

    private static String selfPackageName = "UNKNOWN";
    protected final ClassLoader classLoader;
    protected static Set<String> allowList = new HashSet<>();
    protected static Set<String> enabledFeatures = new HashSet<>(DEFAULT_FEATURES);

    @SuppressLint("StaticFieldLeak")
    protected static Context context = null;
    private static final ArrayList<LegacyHookModule> instances = new ArrayList<>();
    private static boolean isInitReceiver = false;
    public static boolean isBootComplete = false;
    private static Thread loadConfigThread = null;

    protected LegacyHookModule(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        instances.add(this);
        if (instances.size() == 1) {
            initContext(classLoader);
        } else if (context != null && context.getSystemService(UserManager.class).isUserUnlocked()) {
            try {
                onCanReadConfig();
            } catch (Throwable throwable) {
                printLog(throwable.getMessage());
            }
        }
    }

    public static void setSelfPackageName(String packageName) {
        selfPackageName = packageName;
    }

    public static boolean isFeatureEnabledStatic(String prefKey, boolean defaultValue) {
        return enabledFeatures.isEmpty() ? defaultValue : enabledFeatures.contains(prefKey);
    }

    public static boolean isTargetAllowedPackage(String packageName) {
        return packageName != null && ("com.kixyu9527.fcmcommon".equals(packageName) || allowList.contains(packageName));
    }

    private static void initContext(final ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(
            "android.content.ContextWrapper",
            classLoader,
            "attachBaseContext",
            Context.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (context == null) {
                        context = (Context) param.thisObject;
                        if (context.getSystemService(UserManager.class).isUserUnlocked()) {
                            callAllOnCanReadConfig();
                        } else {
                            IntentFilter filter = new IntentFilter();
                            filter.addAction(Intent.ACTION_USER_UNLOCKED);
                            context.registerReceiver(unlockBroadcastReceive, filter);
                        }
                    }
                }
            }
        );
    }

    private static void callAllOnCanReadConfig() {
        initReceiver();
        if ("android".equals(getSelfPackageName())) {
            new Thread(() -> {
                try {
                    Thread.sleep(60_000);
                    isBootComplete = true;
                    printLog("Boot Complete");
                } catch (Throwable throwable) {
                    printLog(throwable.getMessage());
                }
            }).start();
        } else {
            isBootComplete = true;
        }
        for (LegacyHookModule instance : instances) {
            try {
                instance.onCanReadConfig();
            } catch (Throwable throwable) {
                printLog(throwable.getMessage());
            }
        }
    }

    protected void onCanReadConfig() throws Throwable {
    }

    protected static void printLog(String text) {
        printLog(text, false);
    }

    protected static void printLog(String text, boolean diagnostics) {
        if (text == null) {
            return;
        }
        Log.d(TAG, text);
        XposedBridge.log("[FcmCommon] [" + getSelfPackageName() + "] " + text);
        if (diagnostics && context != null) {
            Intent intent = new Intent("com.kooritea.fcmfix.log");
            intent.putExtra("text", "[" + getSelfPackageName() + "]" + text);
            try {
                context.sendBroadcast(intent);
            } catch (Throwable ignored) {
            }
        }
    }

    protected void checkUserDeviceUnlockAndUpdateConfig() {
        if (context != null && context.getSystemService(UserManager.class).isUserUnlocked()) {
            onUpdateConfig();
        }
    }

    private static final BroadcastReceiver unlockBroadcastReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_UNLOCKED.equals(intent.getAction())) {
                try {
                    LegacyHookModule.context.unregisterReceiver(unlockBroadcastReceive);
                } catch (Throwable ignored) {
                }
                callAllOnCanReadConfig();
            }
        }
    };

    protected boolean targetIsAllow(String packageName) {
        if (allowList.isEmpty()) {
            checkUserDeviceUnlockAndUpdateConfig();
        }
        return isTargetAllowedPackage(packageName);
    }

    protected boolean getBooleanConfig(String key, boolean defaultValue) {
        if (enabledFeatures.isEmpty()) {
            checkUserDeviceUnlockAndUpdateConfig();
        }
        return enabledFeatures.isEmpty() ? defaultValue : enabledFeatures.contains(key);
    }

    protected static void onUpdateConfig() {
        if (loadConfigThread == null) {
            loadConfigThread = new Thread(() -> {
                try {
                    SharedPreferences remotePreferences = XposedBridge.getRemotePreferences(ConfigKeys.ConfigGroup);
                    Set<String> remoteAllowList = remotePreferences.getStringSet(ConfigKeys.KeyAllowList, new HashSet<>());
                    Set<String> remoteFeatures = remotePreferences.getStringSet(
                        ConfigKeys.KeyEnabledFeatures,
                        new HashSet<>(DEFAULT_FEATURES)
                    );
                    allowList = remoteAllowList != null ? new HashSet<>(remoteAllowList) : new HashSet<>();
                    enabledFeatures = remoteFeatures != null ? new HashSet<>(remoteFeatures) : new HashSet<>(DEFAULT_FEATURES);
                } catch (Throwable throwable) {
                    printLog("读取远程配置失败: " + throwable.getMessage());
                }
                loadConfigThread = null;
            });
            loadConfigThread.start();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private static synchronized void initReceiver() {
        if (!isInitReceiver && context != null) {
            isInitReceiver = true;
            IntentFilter updateFilter = new IntentFilter();
            updateFilter.addAction(ConfigKeys.UpdateConfigAction);
            BroadcastReceiver updateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (ConfigKeys.UpdateConfigAction.equals(intent.getAction())) {
                        onUpdateConfig();
                    }
                }
            };
            if (Build.VERSION.SDK_INT >= 34) {
                context.registerReceiver(updateReceiver, updateFilter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(updateReceiver, updateFilter);
            }
        }
    }

    protected void sendNotification(String title) {
        sendNotification(title, null, null);
    }

    protected void sendNotification(String title, String content) {
        sendNotification(title, content, null);
    }

    @SuppressLint("MissingPermission")
    protected void sendNotification(String title, String content, PendingIntent pendingIntent) {
        if (context == null) {
            return;
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        createChannel(manager);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("[FcmCommon] " + title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (pendingIntent != null) {
            notification.setContentIntent(pendingIntent).setAutoCancel(true);
        }
        manager.notify((int) System.currentTimeMillis(), notification.build());
    }

    protected void createChannel(NotificationManagerCompat manager) {
        if (context == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "FcmCommon",
            NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("[xposed] FcmCommon");
        manager.createNotificationChannel(channel);
    }

    protected boolean isFCMAction(String action) {
        return action != null && (action.endsWith(".android.c2dm.intent.RECEIVE")
            || "com.google.firebase.MESSAGING_EVENT".equals(action)
            || "com.google.firebase.INSTANCE_ID_EVENT".equals(action));
    }

    protected boolean isFCMIntent(Intent intent) {
        return intent != null && isFCMAction(intent.getAction());
    }

    protected String resolveTargetPackage(Intent intent) {
        if (intent == null) {
            return null;
        }
        if (intent.getComponent() != null) {
            return intent.getComponent().getPackageName();
        }
        return intent.getPackage();
    }

    protected boolean isGmsCorePackage(String packageName) {
        return "com.google.android.gms".equals(packageName)
            || "com.google.android.gms.persistent".equals(packageName);
    }

    protected static String getSelfPackageName() {
        return selfPackageName;
    }
}
