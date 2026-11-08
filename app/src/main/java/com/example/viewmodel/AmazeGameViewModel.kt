package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundAndHapticManager
import com.example.data.AmazeDatabase
import com.example.data.GameRepository
import com.example.data.LevelProgressEntity
import com.example.data.PlayerProfileEntity
import com.example.data.ShopPurchaseOutcome
import com.example.engine.AppThemeMode
import com.example.engine.ArrowPuzzleSolver
import com.example.engine.ArrowTipStyle
import com.example.engine.BoardShapeMask
import com.example.engine.BoardThemePalette
import com.example.engine.DifficultyTier
import com.example.engine.GridPoint
import com.example.engine.LevelAlgorithmArchetype
import com.example.engine.MoveCheckResult
import com.example.engine.PuzzleLevel
import com.example.engine.SmartLevelGenerator
import com.example.engine.WindingArrow
import com.example.security.SecurityAuditReport
import com.example.security.SecurityShieldManager
import com.example.update.GitHubAppUpdater
import com.example.update.GitHubUpdateCheckState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen {
    MAIN_MENU,
    LEVEL_SELECT,
    CUSTOM_GENERATOR,
    GAMEPLAY
}

data class EscapingArrowState(
    val arrow: WindingArrow,
    val totalStepsToClear: Float,
    val startedAtMs: Long = System.currentTimeMillis()
)

data class BlockedArrowFeedback(
    val arrowId: Int,
    val blockerArrowId: Int,
    val collisionPoint: GridPoint,
    val clearStepsBeforeCollision: Int,
    val triggeredAtMs: Long = System.currentTimeMillis()
)

data class GameplayUiState(
    val currentScreen: AppScreen = AppScreen.MAIN_MENU,
    val selectedTierTab: DifficultyTier = DifficultyTier.EASY,
    val customDifficulty: DifficultyTier = DifficultyTier.HARD,
    val customShapeMask: BoardShapeMask = BoardShapeMask.FULL_MATRIX,
    val customArchetype: LevelAlgorithmArchetype = LevelAlgorithmArchetype.HYBRID_GRANDMASTER,
    val previewCustomLevel: PuzzleLevel? = null,
    val isGeneratingLevel: Boolean = false,
    val activeLevel: PuzzleLevel? = null,
    val remainingArrows: List<WindingArrow> = emptyList(),
    val undoStack: List<WindingArrow> = emptyList(),
    val escapingArrows: List<EscapingArrowState> = emptyList(),
    val blockedFeedback: BlockedArrowFeedback? = null,
    val selectedPreviewArrowId: Int? = null,
    val previewModeEnabled: Boolean = false,
    val hintedArrowId: Int? = null,
    val pathInspectCooldownSeconds: Int = 0,
    val hintCooldownSeconds: Int = 0,
    val autoSolveCooldownSeconds: Int = 0,
    val retryEscalationCount: Int = 0,
    val topToastBanner: String? = null,
    val encouragementBanner: String? = null,
    val livesRemaining: Int = 5,
    val maxLives: Int = 5,
    val comboCount: Int = 0,
    val currentLevelScore: Int = 0,
    val elapsedSeconds: Int = 0,
    val levelTimeLimitSeconds: Int = 120,
    val remainingSeconds: Int = 120,
    val isTimeOutGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val earnedStarsOnVictory: Int = 3,
    val isGameOver: Boolean = false,
    val isAutoSolving: Boolean = false,
    val usedAutoSolveInCurrentLevel: Boolean = false,
    val showSettingsModal: Boolean = false,
    val showThemeModal: Boolean = false,
    val showShopModal: Boolean = false,
    val showEyeCareReminder: Boolean = false,
    val showSecurityShieldModal: Boolean = false,
    val securityAuditReport: SecurityAuditReport? = null,
    val screenPrivacyProtectionEnabled: Boolean = false,
    val shopFeedbackMessage: String? = null,
    val updateCheckState: GitHubUpdateCheckState = GitHubUpdateCheckState(),
    val zoomResetTrigger: Int = 0
)

class AmazeGameViewModel(application: Application) : AndroidViewModel(application) {

    private val database by lazy { AmazeDatabase.getInstance(application) }
    private val repository by lazy { GameRepository(database.gameProgressDao()) }
    val soundManager = SoundAndHapticManager(application)

    private val _playerProfile = MutableStateFlow(
        PlayerProfileEntity(highestUnlockedLevel = 1, currentActiveLevel = 1)
    )
    val playerProfile: StateFlow<PlayerProfileEntity> = _playerProfile.asStateFlow()

    private val _levelProgressMap = MutableStateFlow<Map<Int, LevelProgressEntity>>(emptyMap())
    val levelProgressMap: StateFlow<Map<Int, LevelProgressEntity>> = _levelProgressMap.asStateFlow()

    private val _uiState = MutableStateFlow(GameplayUiState())
    val uiState: StateFlow<GameplayUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var autoSolveJob: Job? = null
    private var toastDismissJob: Job? = null
    private var encouragementDismissJob: Job? = null
    private var previewAutoHideJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureInitialized()
            launch {
                repository.playerProfile.collect { profile ->
                    if (profile != null) {
                        _playerProfile.value = profile
                        soundManager.soundEnabled = profile.soundEnabled
                        soundManager.vibrationEnabled = profile.vibrationEnabled
                        soundManager.musicEnabled = profile.musicEnabled
                    }
                }
            }
            launch {
                repository.allLevelProgress.collect { list ->
                    _levelProgressMap.value = list.associateBy { it.levelNumber }
                }
            }
            launch {
                val (vName, vCode) = GitHubAppUpdater.getInstalledVersionInfo(getApplication())
                val initialAudit = SecurityShieldManager.performFullSecurityAudit(getApplication())
                _uiState.update {
                    it.copy(
                        securityAuditReport = initialAudit,
                        updateCheckState = it.updateCheckState.copy(
                            installedVersionName = vName,
                            installedVersionCode = vCode
                        )
                    )
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen != AppScreen.GAMEPLAY) {
            persistCurrentActiveLevelProgressIfAny()
            stopTimer()
            stopAutoSolve()
            soundManager.stopAmbientMusic()
        }
        _uiState.update {
            it.copy(
                currentScreen = screen,
                showSettingsModal = false,
                showThemeModal = false,
                showEyeCareReminder = false,
                showSecurityShieldModal = false
            )
        }
        if (screen == AppScreen.CUSTOM_GENERATOR && _uiState.value.previewCustomLevel == null && !_uiState.value.isGeneratingLevel) {
            generateCustomPreview()
        }
    }

    private fun persistCurrentActiveLevelProgressIfAny() {
        val state = _uiState.value
        val level = state.activeLevel ?: return
        if (level.isCustomGenerated || state.isVictory || state.isGameOver) return
        val remaining = state.remainingArrows
        if (remaining.isNotEmpty() && remaining.size < level.arrows.size) {
            val ids = remaining.map { it.id }
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveActiveLevelProgress(
                    levelNumber = level.levelNumber,
                    remainingArrowIds = ids,
                    remainingSeconds = state.remainingSeconds,
                    elapsedSeconds = state.elapsedSeconds,
                    livesRemaining = state.livesRemaining
                )
            }
        }
    }

    fun selectTierTab(tier: DifficultyTier) {
        _uiState.update { it.copy(selectedTierTab = tier) }
    }

    /**
     * Bottom-bar "فحص المسار" action:
     * Gives a 3-second dotted path preview for a key arrow, then locks for 3 minutes (180 seconds).
     */
    fun togglePreviewMode() {
        val state = _uiState.value
        val level = state.activeLevel ?: return
        if (state.remainingArrows.isEmpty() || state.isVictory || state.isGameOver) return

        if (state.pathInspectCooldownSeconds > 0) {
            val mins = state.pathInspectCooldownSeconds / 60
            val secs = state.pathInspectCooldownSeconds % 60
            _uiState.update {
                it.copy(
                    showShopModal = true,
                    shopFeedbackMessage = "⏳ فحص المسار في وقت انتظار (%02d:%02d) • يمكنك فتح التلميح والمسار والحل التلقائي فوراً بالنقاط!".format(mins, secs),
                    topToastBanner = "⏳ المسار %02d:%02d • افتحه بالنقاط ⭐".format(mins, secs)
                )
            }
            scheduleToastDismiss(2200L)
            return
        }

        val targetArrow = ArrowPuzzleSolver.findBestHintArrow(
            level.gridWidth,
            level.gridHeight,
            state.remainingArrows
        ) ?: state.remainingArrows.firstOrNull() ?: return

        soundManager.playArrowPreviewSelect()
        _uiState.update {
            it.copy(
                selectedPreviewArrowId = targetArrow.id,
                pathInspectCooldownSeconds = 180, // 3 minutes cooldown
                topToastBanner = null
            )
        }
        schedulePreviewAutoHide(7000L)
    }

    fun clearPreviewArrow() {
        previewAutoHideJob?.cancel()
        if (_uiState.value.selectedPreviewArrowId != null) {
            _uiState.update { it.copy(selectedPreviewArrowId = null) }
        }
    }

    /**
     * Generates a new per-player DNA seed so the user can experience how levels
     * change from person to person while remaining 100% solvable.
     */
    fun randomizePlayerDna() {
        viewModelScope.launch(Dispatchers.IO) {
            val newDna = repository.randomizePlayerDnaSeed()
            val curActive = _uiState.value.activeLevel
            if (_uiState.value.currentScreen == AppScreen.GAMEPLAY && curActive != null) {
                val regenerated = withContext(Dispatchers.Default) {
                    SmartLevelGenerator.generateCampaignLevel(curActive.levelNumber, newDna)
                }
                withContext(Dispatchers.Main) {
                    launchLevelIntoGameplay(regenerated)
                }
            } else if (_uiState.value.currentScreen == AppScreen.CUSTOM_GENERATOR) {
                generateCustomPreview()
            }
        }
    }

    /**
     * Starts [levelNumber] ONLY if it is <= `highestUnlockedLevel`.
     * The player can always go back to any previously passed level, but cannot skip forward!
     */
    private fun resolveLockedDifficultyTier(): DifficultyTier? {
        val lockedName = playerProfile.value.lockedDifficultyTierName
        if (lockedName.isBlank()) return null
        return DifficultyTier.entries.firstOrNull { it.name == lockedName }
    }

    fun startCampaignLevel(levelNumber: Int, allowJustUnlocked: Boolean = false) {
        val highestUnlocked = playerProfile.value.highestUnlockedLevel
        if (!allowJustUnlocked && levelNumber > highestUnlocked) {
            _uiState.update {
                it.copy(
                    topToastBanner = "🔒 أكمل المستوى $highestUnlocked أولاً"
                )
            }
            scheduleToastDismiss(2200L)
            return
        }

        stopAutoSolve()
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingLevel = true, retryEscalationCount = 0) }
            val dnaSeed = playerProfile.value.playerDnaSeed
            val lockedTier = resolveLockedDifficultyTier()
            val bonusBankSeconds = withContext(Dispatchers.IO) {
                repository.consumeBonusTimeBank()
            }
            val savedProgress = withContext(Dispatchers.IO) {
                repository.getLevelProgress(levelNumber)
            }
            val generated = withContext(Dispatchers.Default) {
                SmartLevelGenerator.generateCampaignLevel(
                    levelNumber = levelNumber,
                    playerDnaSeed = dnaSeed,
                    escalationSteps = 0,
                    forcedDifficultyTier = lockedTier
                )
            }
            withContext(Dispatchers.IO) {
                repository.setActiveLevel(levelNumber)
            }

            // Check if user has an unfinished session to resume from where they left off
            val savedIds = if (savedProgress?.hasSavedProgress == true && !savedProgress.savedRemainingArrowIdsCsv.isNullOrBlank()) {
                savedProgress.savedRemainingArrowIdsCsv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()
            } else {
                emptySet()
            }
            val restoredArrows = if (savedIds.isNotEmpty()) {
                generated.arrows.filter { it.id in savedIds }
            } else {
                emptyList()
            }

            if (restoredArrows.isNotEmpty() && restoredArrows.size < generated.arrows.size) {
                launchLevelIntoGameplay(
                    level = generated,
                    escalationCount = 0,
                    extraPurchasedSeconds = bonusBankSeconds,
                    customRemainingArrows = restoredArrows,
                    customRemainingSeconds = savedProgress!!.savedRemainingSeconds.coerceAtLeast(20),
                    customElapsedSeconds = savedProgress.savedElapsedSeconds,
                    customLivesRemaining = savedProgress.savedLivesRemaining.coerceIn(1, 5),
                    customBannerMessage = "تم استئناف المستوى $levelNumber (${restoredArrows.size} سهم متبقي)"
                )
            } else {
                launchLevelIntoGameplay(
                    level = generated,
                    escalationCount = 0,
                    extraPurchasedSeconds = bonusBankSeconds
                )
            }
        }
    }

    /**
     * Navigates back to the previous level (always allowed for any level > 1).
     */
    fun goToPreviousLevel() {
        val cur = _uiState.value.activeLevel ?: return
        if (cur.isCustomGenerated || cur.levelNumber <= 1) return
        startCampaignLevel(cur.levelNumber - 1)
    }

    /**
     * Navigates forward ONLY if the next level has already been unlocked (`levelNumber < highestUnlockedLevel`).
     * Cannot skip past `highestUnlockedLevel` without completing it first!
     */
    fun goToNextUnlockedLevel() {
        val cur = _uiState.value.activeLevel ?: return
        if (cur.isCustomGenerated) return
        val highestUnlocked = playerProfile.value.highestUnlockedLevel
        if (cur.levelNumber < highestUnlocked) {
            startCampaignLevel(cur.levelNumber + 1)
        } else {
            _uiState.update {
                it.copy(
                    topToastBanner = "🔒 أكمل المستوى ${cur.levelNumber} أولاً"
                )
            }
            scheduleToastDismiss(2200L)
        }
    }

    fun updateCustomGeneratorOptions(
        difficulty: DifficultyTier = _uiState.value.customDifficulty,
        shapeMask: BoardShapeMask = _uiState.value.customShapeMask,
        archetype: LevelAlgorithmArchetype = _uiState.value.customArchetype
    ) {
        _uiState.update {
            it.copy(
                customDifficulty = difficulty,
                customShapeMask = shapeMask,
                customArchetype = archetype
            )
        }
        generateCustomPreview()
    }

    fun generateCustomPreview() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingLevel = true) }
            val diff = _uiState.value.customDifficulty
            val mask = _uiState.value.customShapeMask
            val arch = _uiState.value.customArchetype
            val dnaSeed = playerProfile.value.playerDnaSeed
            val seed = System.currentTimeMillis() xor (diff.ordinal * 99991L + mask.ordinal * 31337L + arch.ordinal * 13337L)
            val generated = withContext(Dispatchers.Default) {
                SmartLevelGenerator.generateCustomLevel(
                    difficulty = diff,
                    shapeMask = mask,
                    archetype = arch,
                    playerDnaSeed = dnaSeed,
                    seed = seed
                )
            }
            _uiState.update {
                it.copy(
                    previewCustomLevel = generated,
                    isGeneratingLevel = false
                )
            }
        }
    }

    fun startCustomPreviewLevel() {
        val preview = _uiState.value.previewCustomLevel ?: return
        launchLevelIntoGameplay(preview, escalationCount = 0)
    }

    private fun launchLevelIntoGameplay(
        level: PuzzleLevel,
        escalationCount: Int = _uiState.value.retryEscalationCount,
        customBannerMessage: String? = null,
        extraPurchasedSeconds: Int = 0,
        customRemainingArrows: List<WindingArrow>? = null,
        customRemainingSeconds: Int? = null,
        customElapsedSeconds: Int? = null,
        customLivesRemaining: Int? = null
    ) {
        val initialMovableCount = level.solverMetrics.initialMovableCount
        val baseTimeLimit = (level.arrows.size * 3 + 50).coerceIn(60, 420)
        val totalTimeLimit = baseTimeLimit + extraPurchasedSeconds.coerceAtLeast(0)
        val lockedTier = resolveLockedDifficultyTier()
        val modeNote = if (lockedTier != null) {
            "وضع ${lockedTier.arabicLabel} 🔒"
        } else {
            "وضع ${level.difficulty.arabicLabel}"
        }
        val levelTitle = if (level.isCustomGenerated) "مخصص" else "المستوى ${level.levelNumber}"
        val bannerText = customBannerMessage
            ?: "$levelTitle • $modeNote"
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.GAMEPLAY,
                isGeneratingLevel = false,
                activeLevel = level,
                retryEscalationCount = escalationCount,
                remainingArrows = customRemainingArrows ?: level.arrows,
                undoStack = emptyList(),
                escapingArrows = emptyList(),
                blockedFeedback = null,
                selectedPreviewArrowId = null,
                hintedArrowId = null,
                topToastBanner = bannerText,
                encouragementBanner = null,
                livesRemaining = customLivesRemaining ?: 5,
                maxLives = 5,
                comboCount = 0,
                currentLevelScore = 0,
                elapsedSeconds = customElapsedSeconds ?: 0,
                levelTimeLimitSeconds = totalTimeLimit,
                remainingSeconds = customRemainingSeconds ?: totalTimeLimit,
                isTimeOutGameOver = false,
                isVictory = false,
                isGameOver = false,
                isAutoSolving = false,
                usedAutoSolveInCurrentLevel = false,
                showSettingsModal = false,
                showThemeModal = false,
                showShopModal = false,
                showEyeCareReminder = false,
                zoomResetTrigger = it.zoomResetTrigger + 1
            )
        }
        scheduleToastDismiss(3200L)
        startTimer()
        if (playerProfile.value.musicEnabled) {
            soundManager.startAmbientMusicIfNeeded()
        }
    }

    /**
     * Escalates the current level to a harder difficulty mode with a freshly generated denser board
     * and 5 full attempts (`5 محاولات`), UNLESS the player has purchased "ميزة البقاء في وضع معين"
     * (`lockedDifficultyTierName`), in which case it keeps them in their chosen mode!
     */
    private fun escalateAndReopenSameLevel(reasonPrefix: String) {
        val state = _uiState.value
        val curLevel = state.activeLevel ?: return
        stopAutoSolve()
        stopTimer()

        val lockedTier = resolveLockedDifficultyTier()
        val nextEscalation = if (lockedTier != null) 0 else (state.retryEscalationCount + 1)
        val dnaSeed = playerProfile.value.playerDnaSeed

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingLevel = true) }
            val bonusBankSeconds = withContext(Dispatchers.IO) {
                repository.consumeBonusTimeBank()
            }
            val nextLevelBoard = withContext(Dispatchers.Default) {
                if (curLevel.isCustomGenerated) {
                    val targetDiff = lockedTier ?: DifficultyTier.entries[
                        (curLevel.difficulty.ordinal + 1).coerceAtMost(DifficultyTier.entries.lastIndex)
                    ]
                    SmartLevelGenerator.generateCustomLevel(
                        difficulty = targetDiff,
                        shapeMask = curLevel.shapeMask,
                        archetype = curLevel.algorithmArchetype,
                        playerDnaSeed = dnaSeed,
                        seed = System.currentTimeMillis() xor ((nextEscalation + 1) * 7919L)
                    )
                } else {
                    SmartLevelGenerator.generateCampaignLevel(
                        levelNumber = curLevel.levelNumber,
                        playerDnaSeed = dnaSeed,
                        escalationSteps = nextEscalation,
                        forcedDifficultyTier = lockedTier
                    )
                }
            }
            val msg = if (lockedTier != null) {
                "🔄 المستوى ${curLevel.levelNumber} • ${lockedTier.arabicLabel}"
            } else {
                "🔥 المستوى ${curLevel.levelNumber} • وضع ${nextLevelBoard.difficulty.arabicLabel}"
            }
            launchLevelIntoGameplay(
                level = nextLevelBoard,
                escalationCount = nextEscalation,
                customBannerMessage = msg,
                extraPurchasedSeconds = bonusBankSeconds
            )
        }
    }

    fun restartCurrentLevel() {
        val state = _uiState.value
        val level = state.activeLevel ?: return
        stopAutoSolve()
        if (!level.isCustomGenerated) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.clearActiveLevelProgress(level.levelNumber)
            }
        }
        // If the player lost at least one attempt (or ran out of attempts/time), starting anew switches to a harder mode (unless mode-lock was purchased)!
        if (state.livesRemaining < state.maxLives || state.isGameOver) {
            escalateAndReopenSameLevel("🔥 إعادة")
        } else {
            launchLevelIntoGameplay(level, escalationCount = state.retryEscalationCount)
        }
    }

    /**
     * Triggered ONLY when the player presses and holds stationary (ضغط مستمر ثابت) on an arrow:
     * Shows the light dotted semi-transparent black trajectory line for 3 seconds, then hides it automatically.
     */
    fun onArrowLongPressed(arrowId: Int) {
        val state = _uiState.value
        if (state.isVictory || state.isGameOver) return
        val targetArrow = state.remainingArrows.firstOrNull { it.id == arrowId } ?: return
        val level = state.activeLevel ?: return

        soundManager.playArrowPreviewSelect()
        _uiState.update {
            it.copy(
                selectedPreviewArrowId = arrowId,
                blockedFeedback = null,
                topToastBanner = null
            )
        }
        schedulePreviewAutoHide(7000L)
    }

    /**
     * Called when the player releases their finger after a press-and-hold gesture.
     * Keeps the thin laser path visible for 3.5 seconds so player can trace it, then cleanly auto-hides.
     */
    fun onArrowHoldReleased(arrowId: Int) {
        schedulePreviewAutoHide(3500L)
    }

    /**
     * Handles user tapping on an arrow on the board:
     * Single tap immediately executes the arrow move.
     */
    fun onArrowTapped(arrowId: Int) {
        val state = _uiState.value
        if (state.isVictory || state.isGameOver) return
        if (state.remainingArrows.none { it.id == arrowId }) return

        if (arrowId == state.hintedArrowId || arrowId == state.selectedPreviewArrowId) {
            _uiState.update { it.copy(hintedArrowId = null, selectedPreviewArrowId = null) }
        }
        previewAutoHideJob?.cancel()
        executeArrowMove(arrowId)
    }

    private fun formatRemainingAttemptsText(remaining: Int): String {
        return when (remaining) {
            1 -> "بقيت محاولة واحدة"
            2 -> "بقيت محاولتان"
            else -> "بقيت $remaining محاولات"
        }
    }

    private fun executeArrowMove(arrowId: Int) {
        val state = _uiState.value
        val level = state.activeLevel ?: return
        if (state.isVictory || state.isGameOver) return

        val tappedArrow = state.remainingArrows.firstOrNull { it.id == arrowId } ?: return
        val moveResult = ArrowPuzzleSolver.checkMove(
            gridWidth = level.gridWidth,
            gridHeight = level.gridHeight,
            activeArrows = state.remainingArrows,
            arrow = tappedArrow
        )

        when (moveResult) {
            is MoveCheckResult.Clear -> {
                val newCombo = state.comboCount + 1
                val pointsEarned = 25 * newCombo + tappedArrow.length * 4
                val updatedRemaining = state.remainingArrows.filterNot { it.id == arrowId }
                val escapingItem = EscapingArrowState(
                    arrow = tappedArrow,
                    totalStepsToClear = moveResult.stepsToEdge.toFloat().coerceAtLeast(6f),
                    startedAtMs = System.currentTimeMillis()
                )

                soundManager.playArrowEscape(newCombo)

                val isGoldenArrow = tappedArrow.colorAccentIndex == 1
                val arrowBonus = if (isGoldenArrow) 35 else 0
                val totalPointsEarned = pointsEarned + arrowBonus

                // Credit earned points immediately to the player's spendable balance
                if (!state.usedAutoSolveInCurrentLevel) {
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.addScorePoints(totalPointsEarned)
                    }
                }

                val encouragement = when {
                    isGoldenArrow -> "⭐ أحسنت! سهم ذهبي (+35 نقطة إضافية)"
                    newCombo == 3 -> "أبدعت! سلسلة متتالية 🔥"
                    newCombo == 5 -> "نصر مذهل! حركة عبقرية ⚡"
                    newCombo == 8 -> "رائع جداً! استراتيجية متقنة 🎯"
                    newCombo > 0 && newCombo % 10 == 0 -> "أنت بطل المتاهات! احتراف ×$newCombo 🌟"
                    else -> null
                }

                _uiState.update { current ->
                    current.copy(
                        remainingArrows = updatedRemaining,
                        undoStack = current.undoStack + tappedArrow,
                        escapingArrows = current.escapingArrows + escapingItem,
                        blockedFeedback = null,
                        selectedPreviewArrowId = if (current.selectedPreviewArrowId == arrowId) null else current.selectedPreviewArrowId,
                        hintedArrowId = if (current.hintedArrowId == arrowId) null else current.hintedArrowId,
                        comboCount = newCombo,
                        currentLevelScore = current.currentLevelScore + totalPointsEarned,
                        encouragementBanner = encouragement ?: current.encouragementBanner
                    )
                }

                if (encouragement != null) {
                    scheduleEncouragementDismiss(2400L)
                }

                viewModelScope.launch {
                    delay(460L)
                    _uiState.update { cur ->
                        cur.copy(
                            escapingArrows = cur.escapingArrows.filterNot { it.arrow.id == arrowId }
                        )
                    }
                }

                if (updatedRemaining.isEmpty()) {
                    onLevelCleared()
                } else if (!level.isCustomGenerated) {
                    val ids = updatedRemaining.map { it.id }
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.saveActiveLevelProgress(
                            levelNumber = level.levelNumber,
                            remainingArrowIds = ids,
                            remainingSeconds = state.remainingSeconds,
                            elapsedSeconds = state.elapsedSeconds,
                            livesRemaining = state.livesRemaining
                        )
                    }
                }
            }

            is MoveCheckResult.Blocked -> {
                soundManager.playArrowBlocked()
                val newLives = (state.livesRemaining - 1).coerceAtLeast(0)
                val feedback = BlockedArrowFeedback(
                    arrowId = arrowId,
                    blockerArrowId = moveResult.blockerArrowId,
                    collisionPoint = moveResult.collisionPoint,
                    clearStepsBeforeCollision = moveResult.clearStepsBeforeCollision,
                    triggeredAtMs = System.currentTimeMillis()
                )

                if (newLives == 0) {
                    stopTimer()
                    stopAutoSolve()
                    // Show the Out-of-Attempts dialog asking if the player wants to buy attempts
                    // or buy staying in the same level/mode without difficulty escalation!
                    _uiState.update { current ->
                        current.copy(
                            blockedFeedback = feedback,
                            selectedPreviewArrowId = null,
                            livesRemaining = 0,
                            comboCount = 0,
                            isTimeOutGameOver = false,
                            isGameOver = true,
                            shopFeedbackMessage = null
                        )
                    }
                } else {
                    val attemptsMsg = formatRemainingAttemptsText(newLives)
                    _uiState.update { current ->
                        current.copy(
                            blockedFeedback = feedback,
                            selectedPreviewArrowId = null,
                            livesRemaining = newLives,
                            comboCount = 0,
                            isTimeOutGameOver = false,
                            isGameOver = false,
                            topToastBanner = "⚠️ مغلق • $attemptsMsg"
                        )
                    }
                    scheduleToastDismiss(2000L)

                    viewModelScope.launch {
                        delay(800L)
                        _uiState.update { cur ->
                            if (cur.blockedFeedback?.triggeredAtMs == feedback.triggeredAtMs) {
                                cur.copy(blockedFeedback = null)
                            } else {
                                cur
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Gives 1 hint and then locks for 3 minutes (180 seconds) cooldown (`كل 3 دقائق يعطي مرة تلميح`).
     */
    fun triggerHint() {
        val state = _uiState.value
        val level = state.activeLevel ?: return
        if (state.remainingArrows.isEmpty() || state.isVictory || state.isGameOver) return

        if (state.hintCooldownSeconds > 0) {
            val mins = state.hintCooldownSeconds / 60
            val secs = state.hintCooldownSeconds % 60
            _uiState.update {
                it.copy(
                    showShopModal = true,
                    shopFeedbackMessage = "⏳ التلميح في وقت انتظار (%02d:%02d) • يمكنك فتح التلميح والمسار والحل التلقائي فوراً بالنقاط!".format(mins, secs),
                    topToastBanner = "⏳ التلميح %02d:%02d • افتحه بالنقاط ⭐".format(mins, secs)
                )
            }
            scheduleToastDismiss(2200L)
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.consumeHint()
            }
            val movableArrows = ArrowPuzzleSolver.getMovableArrows(
                level.gridWidth,
                level.gridHeight,
                state.remainingArrows
            )
            val bestArrow = ArrowPuzzleSolver.findBestHintArrow(
                level.gridWidth,
                level.gridHeight,
                state.remainingArrows
            )

            soundManager.playHintChime()

            _uiState.update {
                it.copy(
                    hintedArrowId = bestArrow?.id,
                    selectedPreviewArrowId = bestArrow?.id,
                    hintCooldownSeconds = 180,
                    pathInspectCooldownSeconds = 180,
                    topToastBanner = null
                )
            }
            schedulePreviewAutoHide(8000L)
        }
    }

    fun undoLastMove() {
        val state = _uiState.value
        if (state.undoStack.isEmpty() || state.isVictory) return
        val lastArrow = state.undoStack.last()
        soundManager.playUndoSound()
        _uiState.update {
            it.copy(
                remainingArrows = it.remainingArrows + lastArrow,
                undoStack = it.undoStack.dropLast(1),
                blockedFeedback = null,
                selectedPreviewArrowId = null,
                hintedArrowId = lastArrow.id,
                comboCount = 0
            )
        }
    }

    /**
     * Runs Solver step or full Auto-Solve for demonstration/learning according to a cooldown timer.
     * IMPORTANT: Using Auto-Solve marks `usedAutoSolveInCurrentLevel = true`, which prevents
     * unlocking or advancing to the next level until the user replays and solves the level themselves!
     */
    fun toggleSolverStepOrAutoSolve(continuous: Boolean = false) {
        val state = _uiState.value
        val level = state.activeLevel ?: return
        if (state.remainingArrows.isEmpty() || state.isVictory || state.isGameOver) return

        if (state.isAutoSolving) {
            stopAutoSolve()
            return
        }

        if (state.autoSolveCooldownSeconds > 0) {
            val mins = state.autoSolveCooldownSeconds / 60
            val secs = state.autoSolveCooldownSeconds % 60
            _uiState.update {
                it.copy(
                    showShopModal = true,
                    shopFeedbackMessage = "⏳ الحل التلقائي في وقت انتظار (%02d:%02d) • يمكنك فتح التلميح والمسار والحل التلقائي فوراً بالنقاط!".format(mins, secs),
                    topToastBanner = "⏳ الحل %02d:%02d • افتحه بالنقاط ⭐".format(mins, secs)
                )
            }
            scheduleToastDismiss(2200L)
            return
        }

        // Cooldown duration scales by mode (e.g. 180 seconds = 3 minutes)
        val cooldownForMode = 180

        if (!continuous) {
            val best = ArrowPuzzleSolver.findBestHintArrow(
                level.gridWidth,
                level.gridHeight,
                state.remainingArrows
            )
            if (best != null) {
                _uiState.update {
                    it.copy(
                        usedAutoSolveInCurrentLevel = true,
                        autoSolveCooldownSeconds = cooldownForMode,
                        selectedPreviewArrowId = null,
                        topToastBanner = "✨ خطوة حل مساعد"
                    )
                }
                scheduleToastDismiss(2000L)
                executeArrowMove(best.id)
            }
            return
        }

        _uiState.update {
            it.copy(
                isAutoSolving = true,
                usedAutoSolveInCurrentLevel = true,
                autoSolveCooldownSeconds = cooldownForMode,
                selectedPreviewArrowId = null,
                topToastBanner = "✨ عرض الحل التلقائي"
            )
        }
        scheduleToastDismiss(2200L)
        autoSolveJob = viewModelScope.launch {
            while (isActive) {
                val cur = _uiState.value
                val curLevel = cur.activeLevel ?: break
                if (cur.remainingArrows.isEmpty() || cur.isVictory || cur.isGameOver || !cur.isAutoSolving) {
                    break
                }
                val nextArrow = ArrowPuzzleSolver.findBestHintArrow(
                    curLevel.gridWidth,
                    curLevel.gridHeight,
                    cur.remainingArrows
                ) ?: break
                _uiState.update {
                    it.copy(
                        hintedArrowId = nextArrow.id,
                        selectedPreviewArrowId = null
                    )
                }
                delay(160L)
                executeArrowMove(nextArrow.id)
                delay(210L)
            }
            _uiState.update { it.copy(isAutoSolving = false) }
        }
    }

    /**
     * When attempts or timer run out, reopens the same level in a harder mode with 5 attempts.
     */
    fun refillLivesAndContinue() {
        escalateAndReopenSameLevel("🔥 إعادة فتح المرحلة")
    }

    fun resetBoardZoom() {
        _uiState.update { it.copy(zoomResetTrigger = it.zoomResetTrigger + 1) }
    }

    fun setShowSettingsModal(show: Boolean) {
        _uiState.update { it.copy(showSettingsModal = show) }
        if (show) {
            checkGitHubVersionComparison()
        }
    }

    fun setShowSecurityShieldModal(show: Boolean) {
        if (show) {
            refreshSecurityAudit()
        }
        _uiState.update { it.copy(showSecurityShieldModal = show) }
    }

    fun refreshSecurityAudit() {
        viewModelScope.launch(Dispatchers.Default) {
            val report = SecurityShieldManager.performFullSecurityAudit(getApplication())
            _uiState.update { it.copy(securityAuditReport = report) }
        }
    }

    fun toggleScreenPrivacyProtection(enabled: Boolean) {
        _uiState.update { it.copy(screenPrivacyProtectionEnabled = enabled) }
    }

    fun checkGitHubVersionComparison() {
        val currentCheck = _uiState.value.updateCheckState
        if (currentCheck.isChecking || currentCheck.isDownloading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updateCheckState = it.updateCheckState.copy(
                        isChecking = true,
                        comparisonSummaryAr = "جاري مقارنة الإصدار الحالي (${it.updateCheckState.installedVersionName}) مع مستودع GitHub..."
                    )
                )
            }
            val result = GitHubAppUpdater.checkAndCompareWithGitHub(getApplication())
            _uiState.update {
                it.copy(updateCheckState = result)
            }
        }
    }

    fun downloadAndInstallGitHubUpdate() {
        val state = _uiState.value.updateCheckState
        if (state.isDownloading || state.isChecking) return

        // If a valid update APK was already downloaded and the user just granted system install permission, install it now
        val existingPath = state.readyApkFilePath
        if (!existingPath.isNullOrBlank()) {
            val existingFile = java.io.File(existingPath)
            if (existingFile.exists() && existingFile.length() > 500_000L) {
                val outcome = GitHubAppUpdater.launchSystemInstallerWithPermissions(getApplication(), existingFile)
                val statusMsg = when (outcome) {
                    GitHubAppUpdater.InstallLaunchOutcome.LAUNCHED_INSTALLER ->
                        "✅ تم فتح مثبّت النظام لتثبيت التحديث v${state.remoteVersionName ?: ""}."
                    GitHubAppUpdater.InstallLaunchOutcome.REQUESTED_PERMISSION ->
                        "🔐 يرجى تفعيل خيار «السماح بتثبيت التطبيقات من هذا المصدر» في نافذة الإعدادات، ثم ارجع واضغط «تثبيت التحديث الآن»."
                    GitHubAppUpdater.InstallLaunchOutcome.FAILED ->
                        "⚠️ تعذر تشغيل مثبّت النظام تلقائياً على هاتفك. يمكنك الضغط على «فتح في المتصفح» لتنزيل وتثبيت التحديث مباشرة."
                }
                _uiState.update {
                    it.copy(updateCheckState = it.updateCheckState.copy(comparisonSummaryAr = statusMsg))
                }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updateCheckState = it.updateCheckState.copy(
                        isChecking = true,
                        comparisonSummaryAr = "جاري البحث في رابط GitHub ومقارنة الإصدار الحالي (v${it.updateCheckState.installedVersionName})..."
                    )
                )
            }

            // Always perform a fresh check against GitHub based on the installed version
            val latestInfo = GitHubAppUpdater.checkAndCompareWithGitHub(getApplication())

            // If no newer version exists on GitHub, stop here and show the real comparison result (never fake-download!)
            if (!latestInfo.isUpdateAvailable || !latestInfo.hasDirectApkAsset) {
                _uiState.update {
                    it.copy(updateCheckState = latestInfo.copy(isChecking = false, isDownloading = false))
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    updateCheckState = latestInfo.copy(
                        isChecking = false,
                        isDownloading = true,
                        downloadProgressPercent = 1,
                        downloadedMegabytes = 0f,
                        comparisonSummaryAr = "جاري تنزيل الإصدار الجديد (v${latestInfo.remoteVersionName}) من GitHub بالميجات (MB)..."
                    )
                )
            }

            GitHubAppUpdater.downloadRealGitHubUpdateAndInstall(
                context = getApplication(),
                targetUrl = latestInfo.downloadUrl,
                hasDirectApk = latestInfo.hasDirectApkAsset,
                installedVersion = latestInfo.installedVersionName,
                remoteVersion = latestInfo.remoteVersionName,
                expectedTotalMb = latestInfo.totalMegabytes,
                onProgressMb = { downloadedMb, totalMb, pct ->
                    _uiState.update {
                        it.copy(
                            updateCheckState = it.updateCheckState.copy(
                                isDownloading = true,
                                downloadedMegabytes = downloadedMb,
                                totalMegabytes = totalMb,
                                downloadProgressPercent = pct,
                                comparisonSummaryAr = "جاري تنزيل التحديث من GitHub: ${GitHubAppUpdater.formatMb(downloadedMb)} / ${GitHubAppUpdater.formatMb(totalMb)} ($pct%)"
                            )
                        )
                    }
                },
                onComplete = { success, msg, apkPath ->
                    _uiState.update {
                        it.copy(
                            updateCheckState = it.updateCheckState.copy(
                                isDownloading = false,
                                downloadProgressPercent = if (success) 100 else 0,
                                comparisonSummaryAr = msg,
                                readyApkFilePath = apkPath
                            )
                        )
                    }
                }
            )
        }
    }

    fun setShowThemeModal(show: Boolean) {
        _uiState.update { it.copy(showThemeModal = show) }
    }

    fun setShowShopModal(show: Boolean) {
        _uiState.update {
            it.copy(
                showShopModal = show,
                shopFeedbackMessage = if (!show) null else it.shopFeedbackMessage
            )
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(soundEnabled = enabled)
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(vibrationEnabled = enabled)
        }
    }

    fun toggleMusic(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(musicEnabled = enabled)
            if (enabled && _uiState.value.currentScreen == AppScreen.GAMEPLAY) {
                soundManager.startAmbientMusicIfNeeded()
            } else if (!enabled) {
                soundManager.stopAmbientMusic()
            }
        }
    }

    fun toggleVoiceEncouragement(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(voiceEnabled = enabled)
        }
    }

    fun selectBoardTheme(theme: BoardThemePalette) {
        viewModelScope.launch(Dispatchers.IO) {
            // ONLY changes the arrow and grid colors for the puzzle board.
            // Does NOT hijack the app theme mode chosen in Settings!
            repository.updateSettings(selectedThemeId = theme.id)
        }
    }

    fun selectThemeMode(mode: AppThemeMode) {
        viewModelScope.launch(Dispatchers.IO) {
            // ONLY changes the overall UI theme (LIGHT, DARK, SYSTEM).
            // Does NOT overwrite the board arrow palette!
            repository.updateSettings(themeMode = mode.id)
        }
    }

    /**
     * Purchases +60 seconds of extra time using points.
     * Enforces:
     * - Automatic deduction of points
     * - Rate limiting (max 5 purchases per hour)
     * - Dynamic escalating cost (`computeExtraTimeCost(totalPurchasesCount)`)
     */
    fun purchaseExtraTimeWithPoints() {
        viewModelScope.launch {
            val profile = playerProfile.value
            val cost = GameRepository.computeExtraTimeCost(profile.totalPurchasesCount)
            val isInGameplay = _uiState.value.currentScreen == AppScreen.GAMEPLAY && _uiState.value.activeLevel != null

            val outcome = withContext(Dispatchers.IO) {
                repository.executeShopPurchase(
                    costPoints = cost,
                    addBonusSecondsToBank = if (isInGameplay) 0 else 60
                )
            }

            when (outcome) {
                is ShopPurchaseOutcome.Success -> {
                    soundManager.playHintChime()
                    if (isInGameplay) {
                        _uiState.update {
                            it.copy(
                                remainingSeconds = it.remainingSeconds + 60,
                                levelTimeLimitSeconds = it.levelTimeLimitSeconds + 60,
                                shopFeedbackMessage = "✅ تم شراء +60 ثانية إضافية وخصم $cost نقطة بنجاح! (الشراء ${outcome.purchasesUsedInHour}/5 في الساعة)",
                                topToastBanner = "⏱ +60 ثانية للمؤقت!"
                            )
                        }
                        scheduleToastDismiss(2200L)
                    } else {
                        _uiState.update {
                            it.copy(
                                shopFeedbackMessage = "✅ تم شراء +60 ثانية للمستوى القادم وخصم $cost نقطة! (الشراء ${outcome.purchasesUsedInHour}/5 في الساعة)"
                            )
                        }
                    }
                }
                is ShopPurchaseOutcome.InsufficientPoints -> {
                    soundManager.playArrowBlocked()
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⚠️ رصيد النقاط غير كافٍ! المطلوب ${outcome.required} نقطة ولديك ${outcome.available} نقطة"
                        )
                    }
                }
                is ShopPurchaseOutcome.RateLimited -> {
                    soundManager.playArrowBlocked()
                    val mins = (outcome.retryAfterSeconds / 60).coerceAtLeast(1)
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⏳ وصلت للحد الأقصى (5 عمليات شراء في الساعة). يمكنك الشراء مجدداً بعد $mins دقيقة"
                        )
                    }
                }
            }
        }
    }

    /**
     * Unlocks the 3-minute cooldowns for Hint (التلميح), Path Inspect (فحص المسار),
     * and Auto-Solve (الحل التلقائي) immediately using points, plus +2 extra hints.
     */
    fun purchaseUnlockAllToolsWithPoints() {
        viewModelScope.launch {
            val profile = playerProfile.value
            val cost = GameRepository.computeUnlockToolsCost(profile.totalPurchasesCount)

            val outcome = withContext(Dispatchers.IO) {
                repository.executeShopPurchase(
                    costPoints = cost,
                    addHintsCount = 2
                )
            }

            when (outcome) {
                is ShopPurchaseOutcome.Success -> {
                    soundManager.playHintChime()
                    _uiState.update {
                        it.copy(
                            hintCooldownSeconds = 0,
                            pathInspectCooldownSeconds = 0,
                            autoSolveCooldownSeconds = 0,
                            shopFeedbackMessage = "✅ تم فتح التلميح وفحص المسار والحل التلقائي فوراً (+2 تلميح) وخصم $cost نقطة! (${outcome.purchasesUsedInHour}/5 في الساعة)",
                            topToastBanner = "🔓 فُتحت المساعدات فوراً!"
                        )
                    }
                    scheduleToastDismiss(2200L)
                }
                is ShopPurchaseOutcome.InsufficientPoints -> {
                    soundManager.playArrowBlocked()
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⚠️ رصيد النقاط غير كافٍ! المطلوب ${outcome.required} نقطة ولديك ${outcome.available} نقطة"
                        )
                    }
                }
                is ShopPurchaseOutcome.RateLimited -> {
                    soundManager.playArrowBlocked()
                    val mins = (outcome.retryAfterSeconds / 60).coerceAtLeast(1)
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⏳ وصلت للحد الأقصى (5 عمليات شراء في الساعة). يمكنك الشراء مجدداً بعد $mins دقيقة"
                        )
                    }
                }
            }
        }
    }

    /**
     * Purchases the "Stay in Specific Mode" (`ميزة البقاء في وضع معين`) feature using points.
     * Enforces:
     * - Automatic deduction of points
     * - Rate limiting (max 5 purchases per hour)
     * - Dynamic escalating cost (`computeStayInModeCost(totalPurchasesCount)`)
     */
    fun purchaseStayInModeWithPoints(targetTier: DifficultyTier) {
        viewModelScope.launch {
            val profile = playerProfile.value
            val cost = GameRepository.computeStayInModeCost(profile.totalPurchasesCount)

            val outcome = withContext(Dispatchers.IO) {
                repository.executeShopPurchase(
                    costPoints = cost,
                    newLockedDifficultyTierName = targetTier.name
                )
            }

            when (outcome) {
                is ShopPurchaseOutcome.Success -> {
                    soundManager.playHintChime()
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "✅ تم تفعيل البقاء في وضع (${targetTier.arabicLabel}) وخصم $cost نقطة! (الشراء ${outcome.purchasesUsedInHour}/5 في الساعة)",
                            topToastBanner = "🛡 ثُبّت وضع ${targetTier.arabicLabel}"
                        )
                    }
                    scheduleToastDismiss(2200L)

                    // If currently in gameplay and the active level is a different mode, regenerate current level in the chosen locked mode
                    val curLevel = _uiState.value.activeLevel
                    if (_uiState.value.currentScreen == AppScreen.GAMEPLAY && curLevel != null && curLevel.difficulty != targetTier) {
                        val dnaSeed = playerProfile.value.playerDnaSeed
                        val updatedBoard = withContext(Dispatchers.Default) {
                            SmartLevelGenerator.generateCampaignLevel(
                                levelNumber = curLevel.levelNumber,
                                playerDnaSeed = dnaSeed,
                                escalationSteps = 0,
                                forcedDifficultyTier = targetTier
                            )
                        }
                        launchLevelIntoGameplay(
                            level = updatedBoard,
                            escalationCount = 0,
                            customBannerMessage = "🛡 المستوى ${curLevel.levelNumber} • ${targetTier.arabicLabel}"
                        )
                    }
                }
                is ShopPurchaseOutcome.InsufficientPoints -> {
                    soundManager.playArrowBlocked()
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⚠️ رصيد النقاط غير كافٍ! المطلوب ${outcome.required} نقطة ولديك ${outcome.available} نقطة"
                        )
                    }
                }
                is ShopPurchaseOutcome.RateLimited -> {
                    soundManager.playArrowBlocked()
                    val mins = (outcome.retryAfterSeconds / 60).coerceAtLeast(1)
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⏳ وصلت للحد الأقصى (5 عمليات شراء في الساعة). يمكنك الشراء مجدداً بعد $mins دقيقة"
                        )
                    }
                }
            }
        }
    }

    fun unlockAutomaticModeRotation() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearLockedDifficultyMode()
            }
            _uiState.update {
                it.copy(
                    shopFeedbackMessage = "🔄 تم إلغاء تثبيت الوضع والعودة للتنويع التلقائي للمراحل"
                )
            }
        }
    }

    /**
     * Purchases +5 attempts using points so the player can continue solving the exact same board
     * without losing progress or escalating difficulty.
     */
    fun purchaseAttemptsAndContinue() {
        viewModelScope.launch {
            val profile = playerProfile.value
            val cost = GameRepository.computeExtraAttemptsCost(profile.totalPurchasesCount)

            val outcome = withContext(Dispatchers.IO) {
                repository.executeShopPurchase(costPoints = cost)
            }

            when (outcome) {
                is ShopPurchaseOutcome.Success -> {
                    soundManager.playHintChime()
                    _uiState.update {
                        val addedTime = if (it.remainingSeconds < 45) 60 else 0
                        it.copy(
                            livesRemaining = 5,
                            maxLives = 5,
                            remainingSeconds = it.remainingSeconds + addedTime,
                            levelTimeLimitSeconds = it.levelTimeLimitSeconds + addedTime,
                            isGameOver = false,
                            isTimeOutGameOver = false,
                            blockedFeedback = null,
                            shopFeedbackMessage = "✅ تم شراء 5 محاولات إضافية وخصم $cost نقطة! (${outcome.purchasesUsedInHour}/5 في الساعة)",
                            topToastBanner = "💧 +5 محاولات • تابع اللعب!"
                        )
                    }
                    scheduleToastDismiss(2200L)
                    if (_uiState.value.currentScreen == AppScreen.GAMEPLAY && !_uiState.value.isVictory) {
                        startTimer()
                    }
                }
                is ShopPurchaseOutcome.InsufficientPoints -> {
                    soundManager.playArrowBlocked()
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⚠️ رصيد النقاط غير كافٍ! المطلوب ${outcome.required} نقطة ولديك ${outcome.available} نقطة"
                        )
                    }
                }
                is ShopPurchaseOutcome.RateLimited -> {
                    soundManager.playArrowBlocked()
                    val mins = (outcome.retryAfterSeconds / 60).coerceAtLeast(1)
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⏳ وصلت للحد الأقصى (5 عمليات شراء في الساعة). يمكنك الشراء مجدداً بعد $mins دقيقة"
                        )
                    }
                }
            }
        }
    }

    /**
     * Purchases "Stay in the Same Level & Mode Without Changing" (`شراء البقاء في المستوى نفسه دون تغيير`)
     * when attempts run out, restarting the current level with 5 attempts in its current mode without escalating.
     */
    fun purchaseStayInSameLevelWithoutEscalation() {
        viewModelScope.launch {
            val state = _uiState.value
            val curLevel = state.activeLevel ?: return@launch
            val profile = playerProfile.value
            val cost = GameRepository.computeStayInModeCost(profile.totalPurchasesCount)

            val outcome = withContext(Dispatchers.IO) {
                repository.executeShopPurchase(
                    costPoints = cost,
                    newLockedDifficultyTierName = curLevel.difficulty.name
                )
            }

            when (outcome) {
                is ShopPurchaseOutcome.Success -> {
                    soundManager.playHintChime()
                    launchLevelIntoGameplay(
                        level = curLevel,
                        escalationCount = 0,
                        customBannerMessage = "🛡 المستوى ${curLevel.levelNumber} • وضع ${curLevel.difficulty.arabicLabel}"
                    )
                }
                is ShopPurchaseOutcome.InsufficientPoints -> {
                    soundManager.playArrowBlocked()
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⚠️ رصيد النقاط غير كافٍ! المطلوب ${outcome.required} نقطة ولديك ${outcome.available} نقطة"
                        )
                    }
                }
                is ShopPurchaseOutcome.RateLimited -> {
                    soundManager.playArrowBlocked()
                    val mins = (outcome.retryAfterSeconds / 60).coerceAtLeast(1)
                    _uiState.update {
                        it.copy(
                            shopFeedbackMessage = "⏳ وصلت للحد الأقصى (5 عمليات شراء في الساعة). يمكنك الشراء مجدداً بعد $mins دقيقة"
                        )
                    }
                }
            }
        }
    }

    fun proceedToNextLevel() {
        val state = _uiState.value
        val currentLevel = state.activeLevel ?: return
        // Strict rule: if Auto-Solve was used, the player CANNOT advance to the next level!
        // They must replay and solve the current level themselves.
        if (state.usedAutoSolveInCurrentLevel) {
            launchLevelIntoGameplay(
                level = currentLevel,
                escalationCount = state.retryEscalationCount,
                customBannerMessage = "حل المستوى ${currentLevel.levelNumber} بنفسك"
            )
            return
        }
        if (currentLevel.isCustomGenerated) {
            generateCustomPreview()
            viewModelScope.launch {
                delay(150L)
                startCustomPreviewLevel()
            }
        } else {
            val nextNum = (currentLevel.levelNumber + 1).coerceAtMost(SmartLevelGenerator.TOTAL_CAMPAIGN_LEVELS)
            startCampaignLevel(nextNum, allowJustUnlocked = true)
        }
    }

    private fun onLevelCleared() {
        stopTimer()
        stopAutoSolve()
        val state = _uiState.value
        val level = state.activeLevel ?: return

        val stars = if (state.usedAutoSolveInCurrentLevel) {
            0
        } else {
            when {
                state.livesRemaining >= 4 -> 3
                state.livesRemaining >= 2 -> 2
                else -> 1
            }
        }
        val lifeBonus = if (state.usedAutoSolveInCurrentLevel) 0 else state.livesRemaining * 120
        val finalScore = if (state.usedAutoSolveInCurrentLevel) 0 else state.currentLevelScore + lifeBonus

        soundManager.playVictoryFanfare()

        _uiState.update {
            it.copy(
                isVictory = true,
                earnedStarsOnVictory = stars,
                currentLevelScore = finalScore,
                isAutoSolving = false
            )
        }

        if (!level.isCustomGenerated) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.clearActiveLevelProgress(level.levelNumber)
            }
        }

        // Only unlock the next level and save completion if the player solved it themselves WITHOUT auto-solve!
        // Note: arrow escape points were already added in real time, so we add `lifeBonus` here.
        if (!state.usedAutoSolveInCurrentLevel) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.recordLevelCompleted(
                    levelNumber = level.levelNumber,
                    isCustom = level.isCustomGenerated,
                    starsEarned = stars,
                    scoreEarned = lifeBonus,
                    elapsedSeconds = state.elapsedSeconds
                )
            }
        }
    }

    private var continuousFocusSeconds = 0

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val cur = _uiState.value
                if (cur.currentScreen == AppScreen.GAMEPLAY && !cur.isVictory && !cur.isGameOver && !cur.showSettingsModal && !cur.showShopModal && !cur.showEyeCareReminder) {
                    continuousFocusSeconds++
                    if (continuousFocusSeconds >= 1200) {
                        continuousFocusSeconds = 0
                        _uiState.update { it.copy(showEyeCareReminder = true) }
                    }

                    val nextRem = (cur.remainingSeconds - 1).coerceAtLeast(0)
                    val nextElapsed = cur.elapsedSeconds + 1
                    val nextPathCooldown = (cur.pathInspectCooldownSeconds - 1).coerceAtLeast(0)
                    val nextHintCooldown = (cur.hintCooldownSeconds - 1).coerceAtLeast(0)
                    val nextAutoSolveCooldown = (cur.autoSolveCooldownSeconds - 1).coerceAtLeast(0)
                    if (nextRem == 0) {
                        soundManager.playArrowBlocked()
                        stopAutoSolve()
                        _uiState.update {
                            it.copy(
                                elapsedSeconds = nextElapsed,
                                remainingSeconds = 0,
                                pathInspectCooldownSeconds = nextPathCooldown,
                                hintCooldownSeconds = nextHintCooldown,
                                autoSolveCooldownSeconds = nextAutoSolveCooldown,
                                isTimeOutGameOver = true,
                                isGameOver = true,
                                shopFeedbackMessage = null
                            )
                        }
                        break
                    } else {
                        _uiState.update {
                            it.copy(
                                elapsedSeconds = nextElapsed,
                                remainingSeconds = nextRem,
                                pathInspectCooldownSeconds = nextPathCooldown,
                                hintCooldownSeconds = nextHintCooldown,
                                autoSolveCooldownSeconds = nextAutoSolveCooldown
                            )
                        }
                    }
                }
            }
        }
    }

    fun dismissEyeCareReminder() {
        _uiState.update { it.copy(showEyeCareReminder = false) }
    }

    fun enableEyeProtectionAndDismissReminder() {
        toggleEyeProtection(true)
        dismissEyeCareReminder()
    }

    fun selectArrowTipStyle(style: ArrowTipStyle) {
        viewModelScope.launch {
            repository.updateSettings(arrowTipStyle = style.name)
        }
    }

    fun toggleEyeProtection(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(eyeProtectionMode = enabled)
            _uiState.update {
                it.copy(
                    topToastBanner = if (enabled) "تم تفعيل وضع حماية البصر 🌿 (فلتر دافئ مريح)" else "تم إيقاف وضع حماية البصر"
                )
            }
            delay(2200L)
            _uiState.update { it.copy(topToastBanner = null) }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun stopAutoSolve() {
        autoSolveJob?.cancel()
        autoSolveJob = null
        _uiState.update { it.copy(isAutoSolving = false) }
    }

    private fun schedulePreviewAutoHide(delayMs: Long = 3000L) {
        previewAutoHideJob?.cancel()
        previewAutoHideJob = viewModelScope.launch {
            delay(delayMs)
            _uiState.update { it.copy(selectedPreviewArrowId = null) }
        }
    }

    private fun scheduleToastDismiss(delayMs: Long) {
        toastDismissJob?.cancel()
        toastDismissJob = viewModelScope.launch {
            delay(delayMs)
            _uiState.update { it.copy(topToastBanner = null) }
        }
    }

    private fun scheduleEncouragementDismiss(delayMs: Long) {
        encouragementDismissJob?.cancel()
        encouragementDismissJob = viewModelScope.launch {
            delay(delayMs)
            _uiState.update { it.copy(encouragementBanner = null) }
        }
    }

    fun openUpdateInBrowser() {
        val url = _uiState.value.updateCheckState.downloadUrl
        GitHubAppUpdater.openDirectDownloadInBrowser(getApplication(), url)
    }

    fun onActivityPaused() {
        soundManager.onAppBackgrounded()
    }

    fun onActivityResumed() {
        val isGameplay = _uiState.value.currentScreen == AppScreen.GAMEPLAY
        val musicOn = _playerProfile.value.musicEnabled
        soundManager.onAppForegrounded(shouldPlayMusic = isGameplay && musicOn)
    }

    fun onActivityDestroyed() {
        soundManager.release()
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
