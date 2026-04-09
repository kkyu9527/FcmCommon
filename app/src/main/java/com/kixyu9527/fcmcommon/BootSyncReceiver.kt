package com.kixyu9527.fcmcommon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_SHUTDOWN -> {
                val pendingResult = goAsync()
                Thread {
                    runCatching {
                        context.appContainer.connectionEventRepository.recordBoot(intent.action)
                        if (intent.action != Intent.ACTION_SHUTDOWN) {
                            context.appContainer.configRepository.primeRemoteSync()
                        }
                    }
                    pendingResult.finish()
                }.start()
            }
        }
    }
}
