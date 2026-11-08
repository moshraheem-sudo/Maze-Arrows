package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.AppThemeMode
import com.example.engine.BoardThemePalette
import com.example.security.SecurityShieldManager
import com.example.ui.components.AmazeEyeCareReminderDialog
import com.example.ui.components.AmazePointsShopDialog
import com.example.ui.components.AmazeSecurityShieldDialog
import com.example.ui.components.AmazeSettingsDialog
import com.example.ui.components.AmazeThemeDialog
import com.example.ui.screens.CustomGeneratorScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AmazeGameViewModel
import com.example.viewmodel.AppScreen

class MainActivity : ComponentActivity() {
    private val mainViewModel: AmazeGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityShieldManager.applyAntiTapjacking(this)
        enableEdgeToEdge()
        setContent {
            AmazeArrowsApp(viewModel = mainViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.onActivityResumed()
    }

    override fun onPause() {
        super.onPause()
        mainViewModel.onActivityPaused()
    }

    override fun onStop() {
        super.onStop()
        mainViewModel.onActivityPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainViewModel.onActivityDestroyed()
    }
}

@Composable
fun AmazeArrowsApp(
    viewModel: AmazeGameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.playerProfile.collectAsStateWithLifecycle()
    val levelProgressMap by viewModel.levelProgressMap.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val appThemeMode = AppThemeMode.fromId(profile.themeMode)
    val themePalette = BoardThemePalette.resolveForThemeMode(
        selectedThemeId = profile.selectedThemeId,
        themeMode = appThemeMode,
        isSystemDark = isSystemDark
    )

    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    viewModel.onActivityPaused()
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onActivityResumed()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    viewModel.onActivityDestroyed()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onActivityPaused()
        }
    }

    LaunchedEffect(uiState.screenPrivacyProtectionEnabled) {
        SecurityShieldManager.setScreenCaptureProtection(activity, uiState.screenPrivacyProtectionEnabled)
    }

    MyApplicationTheme(darkTheme = themePalette.isDark) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(themePalette.boardBackgroundHex)
            ) {
                Crossfade(
                    targetState = uiState.currentScreen,
                    label = "screenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.MAIN_MENU -> {
                            MainMenuScreen(
                                profile = profile,
                                themePalette = themePalette,
                                onContinueLevel = { lvl -> viewModel.startCampaignLevel(lvl) },
                                onOpenLevelSelect = { viewModel.navigateTo(AppScreen.LEVEL_SELECT) },
                                onOpenCustomGenerator = { viewModel.navigateTo(AppScreen.CUSTOM_GENERATOR) },
                                onOpenShop = { viewModel.setShowShopModal(true) },
                                onOpenSettings = { viewModel.setShowSettingsModal(true) },
                                onOpenThemes = { viewModel.setShowThemeModal(true) },
                                onOpenSecurityShield = { viewModel.setShowSecurityShieldModal(true) }
                            )
                        }

                        AppScreen.LEVEL_SELECT -> {
                            LevelSelectScreen(
                                selectedTier = uiState.selectedTierTab,
                                levelProgressMap = levelProgressMap,
                                highestUnlockedLevel = profile.highestUnlockedLevel,
                                themePalette = themePalette,
                                onSelectTier = { viewModel.selectTierTab(it) },
                                onStartLevel = { viewModel.startCampaignLevel(it) },
                                onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                            )
                        }

                        AppScreen.CUSTOM_GENERATOR -> {
                            CustomGeneratorScreen(
                                uiState = uiState,
                                themePalette = themePalette,
                                onUpdateOptions = { diff, mask, arch ->
                                    viewModel.updateCustomGeneratorOptions(diff, mask, arch)
                                },
                                onRegenerate = { viewModel.generateCustomPreview() },
                                onPlayPreview = { viewModel.startCustomPreviewLevel() },
                                onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                            )
                        }

                        AppScreen.GAMEPLAY -> {
                            GameplayScreen(
                                uiState = uiState,
                                profile = profile,
                                themePalette = themePalette,
                                onArrowTapped = { arrowId -> viewModel.onArrowTapped(arrowId) },
                                onPreviousLevelClick = { viewModel.goToPreviousLevel() },
                                onNextUnlockedLevelClick = { viewModel.goToNextUnlockedLevel() },
                                onHintClick = { viewModel.triggerHint() },
                                onUndoClick = { viewModel.undoLastMove() },
                                onTogglePreviewMode = { viewModel.togglePreviewMode() },
                                onAutoSolveToggle = { viewModel.toggleSolverStepOrAutoSolve(continuous = true) },
                                onResetZoomClick = { viewModel.resetBoardZoom() },
                                onRestartLevel = { viewModel.restartCurrentLevel() },
                                onNextLevel = { viewModel.proceedToNextLevel() },
                                onRefillLives = { viewModel.refillLivesAndContinue() },
                                onBuyAttemptsAndContinue = { viewModel.purchaseAttemptsAndContinue() },
                                onBuyStayInSameLevel = { viewModel.purchaseStayInSameLevelWithoutEscalation() },
                                onOpenShop = { viewModel.setShowShopModal(true) },
                                onOpenSettings = { viewModel.setShowSettingsModal(true) },
                                onOpenThemes = { viewModel.setShowThemeModal(true) },
                                onBackToMenu = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
                                onArrowLongPressed = { arrowId -> viewModel.onArrowLongPressed(arrowId) },
                                onArrowHoldReleased = { arrowId -> viewModel.onArrowHoldReleased(arrowId) },
                                onClearPreview = { viewModel.clearPreviewArrow() }
                            )
                        }
                    }
                }

                if (uiState.showSettingsModal) {
                    AmazeSettingsDialog(
                        profile = profile,
                        showRestartButton = uiState.currentScreen == AppScreen.GAMEPLAY,
                        themePalette = themePalette,
                        updateCheckState = uiState.updateCheckState,
                        onCheckGitHubUpdate = { viewModel.checkGitHubVersionComparison() },
                        onDownloadGitHubUpdate = { viewModel.downloadAndInstallGitHubUpdate() },
                        onOpenBrowserUpdate = { viewModel.openUpdateInBrowser() },
                        onDismiss = { viewModel.setShowSettingsModal(false) },
                        onToggleSound = { viewModel.toggleSound(it) },
                        onToggleVibration = { viewModel.toggleVibration(it) },
                        onToggleMusic = { viewModel.toggleMusic(it) },
                        onToggleVoice = { viewModel.toggleVoiceEncouragement(it) },
                        onSelectThemeMode = { viewModel.selectThemeMode(it) },
                        onSelectArrowTipStyle = { viewModel.selectArrowTipStyle(it) },
                        onToggleEyeProtection = { viewModel.toggleEyeProtection(it) },
                        screenPrivacyProtection = uiState.screenPrivacyProtectionEnabled,
                        onToggleScreenPrivacyProtection = { viewModel.toggleScreenPrivacyProtection(it) },
                        onOpenSecurityShieldDialog = { viewModel.setShowSecurityShieldModal(true) },
                        onRestartLevel = { viewModel.restartCurrentLevel() }
                    )
                }

                if (uiState.showSecurityShieldModal) {
                    AmazeSecurityShieldDialog(
                        report = uiState.securityAuditReport,
                        isDark = themePalette.isDark,
                        onRefreshAudit = { viewModel.refreshSecurityAudit() },
                        onDismiss = { viewModel.setShowSecurityShieldModal(false) }
                    )
                }

                if (uiState.showEyeCareReminder) {
                    AmazeEyeCareReminderDialog(
                        themePalette = themePalette,
                        eyeProtectionActive = profile.eyeProtectionMode,
                        onEnableEyeProtection = { viewModel.enableEyeProtectionAndDismissReminder() },
                        onDismiss = { viewModel.dismissEyeCareReminder() }
                    )
                }

                if (uiState.showThemeModal) {
                    AmazeThemeDialog(
                        selectedThemeId = profile.selectedThemeId,
                        isDark = themePalette.isDark,
                        onSelectTheme = { viewModel.selectBoardTheme(it) },
                        onDismiss = { viewModel.setShowThemeModal(false) }
                    )
                }

                if (uiState.showShopModal) {
                    AmazePointsShopDialog(
                        profile = profile,
                        feedbackMessage = uiState.shopFeedbackMessage,
                        isDark = themePalette.isDark,
                        onPurchaseExtraTime = { viewModel.purchaseExtraTimeWithPoints() },
                        onPurchaseUnlockTools = { viewModel.purchaseUnlockAllToolsWithPoints() },
                        onPurchaseStayInMode = { tier -> viewModel.purchaseStayInModeWithPoints(tier) },
                        onUnlockAutoMode = { viewModel.unlockAutomaticModeRotation() },
                        onDismiss = { viewModel.setShowShopModal(false) }
                    )
                }
            }
        }
    }
}
