package com.kixyu9527.fcmcommon.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kixyu9527.fcmcommon.appContainer
import com.kixyu9527.fcmcommon.data.AppThemeMode
import com.kixyu9527.fcmcommon.data.FeatureKey
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.appContainer
    private val configRepository = container.configRepository
    private val uiSettingsRepository = container.uiSettingsRepository
    private val appsRepository = container.installedAppsRepository
    private val serviceMonitor = container.xposedServiceMonitor
    private val fcmDiagnosticsRepository = container.fcmDiagnosticsStateRepository
    private val uiStateAssembler = HomeUiStateAssembler(
        nameCollator = Collator.getInstance(Locale.CHINA),
        eventTimeFormatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA),
        appTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA),
    )

    private val initialSettings = uiSettingsRepository.settings.value
    private val initialCachedAppsSnapshot = appsRepository.loadCachedInstalledAppsSnapshot()
    private val initialAppsPermissionGranted = appsRepository.canReadInstalledApps()
    private val initialAppsScanned = initialCachedAppsSnapshot.hasSnapshot
    private val initialDiagnosticsState = fcmDiagnosticsRepository.loadState()
    private val shouldBootstrapScan =
        !initialSettings.hasCompletedInitialScan || !initialCachedAppsSnapshot.hasSnapshot

    private val navigationState = HomeNavigationStateContainer()
    private val runtimeState = HomeRuntimeStateContainer(
        initialAppsPermissionGranted = initialAppsPermissionGranted,
        initialApps = initialCachedAppsSnapshot.apps,
        initialAppsScanned = initialAppsScanned,
        shouldBootstrapScan = shouldBootstrapScan,
        initialDiagnosticsState = initialDiagnosticsState,
    )

    private val uiSeed = combine(
        configRepository.config,
        uiSettingsRepository.settings,
        serviceMonitor.state,
        navigationState.seed,
    ) { config, settings, connection, navigation ->
        HomeUiSeed(
            page = navigation.page,
            secondaryPage = navigation.secondaryPage,
            tertiaryPage = navigation.tertiaryPage,
            settings = settings,
            connection = connection,
            query = navigation.query,
            enabledFeatures = config.enabledFeatures,
            allowList = config.allowList,
            selectedAppPackage = navigation.selectedAppPackage,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        uiSeed,
        runtimeState.seed,
    ) { seed, runtime ->
        uiStateAssembler.build(seed, runtime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = uiStateAssembler.buildInitialState(
            initialSettings = initialSettings,
            initialApps = initialCachedAppsSnapshot.apps,
            initialAppsPermissionGranted = initialAppsPermissionGranted,
            shouldBootstrapScan = shouldBootstrapScan,
            initialAppsScanned = initialAppsScanned,
        ),
    )

    init {
        startClockTicker()
        bootstrapRefresh()
    }

    fun onHostResumed() {
        viewModelScope.launch {
            refreshAppsPermissionState(triggerScanIfNeeded = shouldBootstrapScan)
        }
    }

    fun selectPage(page: AppPage) {
        navigationState.selectPage(page)
    }

    fun openSecondaryPage(page: SecondaryPage) {
        navigationState.openSecondaryPage(page)
    }

    fun openTertiaryPage(page: TertiaryPage) {
        navigationState.openTertiaryPage(page)
    }

    fun openAppDetails(packageName: String) {
        navigationState.openAppDetails(packageName)
    }

    fun navigateBack() {
        navigationState.navigateBack()
    }

    fun setFeatureEnabled(featureKey: FeatureKey, enabled: Boolean) {
        viewModelScope.launch {
            configRepository.setFeatureEnabled(featureKey, enabled)
        }
    }

    fun applyRecommendedFeatures() {
        viewModelScope.launch {
            configRepository.applyRecommendedFeatures()
        }
    }

    fun setSearchQuery(query: String) {
        navigationState.setSearchQuery(query)
    }

    fun setOnlyShowPushApps(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setOnlyShowPushApps(enabled)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            uiSettingsRepository.setThemeMode(themeMode)
        }
    }

    fun setShowSystemApps(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setShowSystemApps(enabled)
        }
    }

    fun setShowPackageNameInList(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setShowPackageNameInList(enabled)
        }
    }

    fun setShowDisabledApps(enabled: Boolean) {
        viewModelScope.launch {
            uiSettingsRepository.setShowDisabledApps(enabled)
        }
    }

    fun toggleAppAllowed(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            configRepository.setAppAllowed(packageName, allowed)
        }
    }

    fun allowAllPushCandidates() {
        viewModelScope.launch {
            val packages = runtimeState.currentInstalledApps()
                .filter { it.hasPushSupport }
                .mapTo(linkedSetOf()) { it.packageName }
            configRepository.replaceAllowList(packages)
        }
    }

    fun clearAllowList() {
        viewModelScope.launch {
            configRepository.clearAllowList()
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            configRepository.refresh()
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            if (runtimeState.canReadInstalledApps()) {
                refreshAppsInternal()
            } else {
                refreshAppsPermissionState(triggerScanIfNeeded = true)
            }
        }
    }

    private fun bootstrapRefresh() {
        viewModelScope.launch {
            configRepository.refresh()
            hydrateCachedIconsIfNeeded()
            refreshAppsPermissionState(triggerScanIfNeeded = shouldBootstrapScan)
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (true) {
                runtimeState.tick(fcmDiagnosticsRepository.loadState())
                delay(1_000L)
            }
        }
    }

    private suspend fun refreshAppsInternal() {
        runtimeState.setAppsLoading(true)
        runCatching {
            appsRepository.refreshInstalledAppsCache()
        }.onSuccess { refreshedApps ->
            runtimeState.setInstalledApps(refreshedApps)
            runtimeState.setAppsScanned(true)
            uiSettingsRepository.markInitialScanCompleted()
        }
        runtimeState.setAppsLoading(false)
    }

    private suspend fun refreshAppsPermissionState(triggerScanIfNeeded: Boolean) {
        val granted = appsRepository.canReadInstalledApps()
        runtimeState.setAppsPermissionGranted(granted)
        if (!granted) {
            runtimeState.clearAppsState()
            return
        }

        if (triggerScanIfNeeded && !runtimeState.isAppsLoading() && !runtimeState.isAppsScanned()) {
            refreshAppsInternal()
        } else {
            hydrateCachedIconsIfNeeded()
        }
    }

    private suspend fun hydrateCachedIconsIfNeeded() {
        val apps = runtimeState.currentInstalledApps()
        if (apps.isEmpty() || apps.none { it.icon == null }) return
        runtimeState.setInstalledApps(appsRepository.hydrateCachedInstalledAppsIcons(apps))
    }
}
