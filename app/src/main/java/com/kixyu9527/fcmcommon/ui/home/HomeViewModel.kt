package com.kixyu9527.fcmcommon.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kixyu9527.fcmcommon.appContainer
import com.kixyu9527.fcmcommon.data.FeatureKey
import com.kixyu9527.fcmcommon.xposed.XposedServiceState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeatureCardModel(
    val key: FeatureKey,
    val enabled: Boolean,
)

data class MigrationTrackModel(
    val title: String,
    val detail: String,
    val source: String,
)

data class HomeUiState(
    val connection: XposedServiceState = XposedServiceState.bootstrap(),
    val features: List<FeatureCardModel> = emptyList(),
    val migrationTracks: List<MigrationTrackModel> = emptyList(),
    val trackedAppsCount: Int = 0,
) {
    val enabledFeatureCount: Int
        get() = features.count { it.enabled }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.appContainer
    private val configRepository = container.configRepository
    private val serviceMonitor = container.xposedServiceMonitor

    val uiState: StateFlow<HomeUiState> = combine(
        configRepository.config,
        serviceMonitor.state,
    ) { config, connection ->
        HomeUiState(
            connection = connection,
            features = FeatureKey.entries.map { featureKey ->
                FeatureCardModel(
                    key = featureKey,
                    enabled = config.isEnabled(featureKey),
                )
            },
            migrationTracks = migrationTracks(),
            trackedAppsCount = config.allowList.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            migrationTracks = migrationTracks(),
        ),
    )

    fun setFeatureEnabled(featureKey: FeatureKey, enabled: Boolean) {
        viewModelScope.launch {
            configRepository.setFeatureEnabled(featureKey, enabled)
        }
    }

    fun refreshConfig() {
        viewModelScope.launch {
            configRepository.refresh()
        }
    }

    private fun migrationTracks(): List<MigrationTrackModel> = listOf(
        MigrationTrackModel(
            title = "System broadcast lane",
            detail = "Merge HyperFCMLive's broadcast deferral bypass with fcmfix's stopped-app delivery path.",
            source = "Hooker + BroadcastFix",
        ),
        MigrationTrackModel(
            title = "HyperOS startup lane",
            detail = "Port ActivityManager and autostart exemptions into a single HyperOS-first policy layer.",
            source = "BroadcastQueueModernStubImpl + AutoStartFix",
        ),
        MigrationTrackModel(
            title = "PowerKeeper lane",
            detail = "Unify GMS network, alarm, and whitelist handling without requiring a second helper module.",
            source = "hookGmsObserver + PowerkeeperFix",
        ),
    )
}
