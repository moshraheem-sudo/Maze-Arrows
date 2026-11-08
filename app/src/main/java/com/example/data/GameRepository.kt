package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Repository abstracting local Room persistence for player profile, unique Player DNA seed,
 * settings, and 500-level campaign progress.
 */
class GameRepository(private val dao: GameProgressDao) {

    val allLevelProgress: Flow<List<LevelProgressEntity>> = dao.observeAllLevelProgress()
    val playerProfile: Flow<PlayerProfileEntity?> = dao.observePlayerProfile()

    suspend fun ensureInitialized() {
        val currentProfile = dao.getPlayerProfileOnce()
        if (currentProfile == null) {
            val uniquePlayerSeed = UUID.randomUUID().mostSignificantBits xor System.nanoTime()
            dao.upsertPlayerProfile(
                PlayerProfileEntity(
                    id = 1,
                    highestUnlockedLevel = 1,
                    currentActiveLevel = 1,
                    playerDnaSeed = uniquePlayerSeed,
                    totalScore = 60,
                    totalStars = 0,
                    hintsBalance = 3,
                    themeMode = "LIGHT"
                )
            )
            dao.upsertLevelProgress(
                LevelProgressEntity(
                    levelNumber = 1,
                    isUnlocked = true,
                    isCompleted = false
                )
            )
        } else if (currentProfile.totalScore > 150) {
            // Automatically rebalance legacy inflated test balance (>150 pts) to disciplined economy (80 pts)
            dao.upsertPlayerProfile(currentProfile.copy(totalScore = 80))
        }
    }

    suspend fun randomizePlayerDnaSeed(): Long {
        val profile = dao.getPlayerProfileOnce() ?: PlayerProfileEntity()
        val newSeed = UUID.randomUUID().mostSignificantBits xor System.nanoTime()
        dao.upsertPlayerProfile(profile.copy(playerDnaSeed = newSeed))
        return newSeed
    }

    suspend fun addScorePoints(points: Int) {
        if (points <= 0) return
        val profile = dao.getPlayerProfileOnce() ?: PlayerProfileEntity()
        dao.upsertPlayerProfile(
            profile.copy(totalScore = profile.totalScore + points)
        )
    }

    suspend fun consumeBonusTimeBank(): Int {
        val profile = dao.getPlayerProfileOnce() ?: return 0
        val bonus = profile.bonusTimeSecondsBank
        if (bonus > 0) {
            dao.upsertPlayerProfile(profile.copy(bonusTimeSecondsBank = 0))
        }
        return bonus
    }

    suspend fun recordLevelCompleted(
        levelNumber: Int,
        isCustom: Boolean,
        starsEarned: Int,
        scoreEarned: Int,
        elapsedSeconds: Int
    ) {
        val profile = dao.getPlayerProfileOnce() ?: PlayerProfileEntity()
        if (isCustom) {
            dao.upsertPlayerProfile(
                profile.copy(
                    totalScore = profile.totalScore + scoreEarned,
                    totalStars = profile.totalStars + starsEarned,
                    hintsBalance = profile.hintsBalance + 1,
                    customLevelsSolved = profile.customLevelsSolved + 1
                )
            )
            return
        }

        val existing = dao.getLevelProgress(levelNumber)
        val previousStars = existing?.starsEarned ?: 0
        val starDiff = max(0, starsEarned - previousStars)
        val bestStars = max(previousStars, starsEarned)
        val bestScore = max(existing?.bestScore ?: 0, scoreEarned)
        val bestTime = if (existing == null || existing.bestTimeSeconds == 0) {
            elapsedSeconds
        } else {
            min(existing.bestTimeSeconds, elapsedSeconds)
        }

        dao.upsertLevelProgress(
            LevelProgressEntity(
                levelNumber = levelNumber,
                isUnlocked = true,
                isCompleted = true,
                starsEarned = bestStars,
                bestScore = bestScore,
                bestTimeSeconds = bestTime,
                updatedAt = System.currentTimeMillis()
            )
        )

        val nextLevel = levelNumber + 1
        val existingNext = dao.getLevelProgress(nextLevel)
        if (existingNext == null) {
            dao.upsertLevelProgress(
                LevelProgressEntity(
                    levelNumber = nextLevel,
                    isUnlocked = true,
                    isCompleted = false
                )
            )
        } else if (!existingNext.isUnlocked) {
            dao.upsertLevelProgress(existingNext.copy(isUnlocked = true))
        }

        val newHighest = max(profile.highestUnlockedLevel, nextLevel)
        dao.upsertPlayerProfile(
            profile.copy(
                highestUnlockedLevel = newHighest,
                currentActiveLevel = nextLevel,
                totalScore = profile.totalScore + scoreEarned,
                totalStars = profile.totalStars + starDiff,
                hintsBalance = profile.hintsBalance + if (starsEarned == 3) 1 else 0
            )
        )
    }

    suspend fun setActiveLevel(levelNumber: Int) {
        val profile = dao.getPlayerProfileOnce() ?: PlayerProfileEntity()
        val existing = dao.getLevelProgress(levelNumber)
        if (existing == null) {
            dao.upsertLevelProgress(
                LevelProgressEntity(
                    levelNumber = levelNumber,
                    isUnlocked = true,
                    isCompleted = false
                )
            )
        } else if (!existing.isUnlocked) {
            dao.upsertLevelProgress(existing.copy(isUnlocked = true))
        }
        dao.upsertPlayerProfile(
            profile.copy(
                currentActiveLevel = levelNumber,
                highestUnlockedLevel = max(profile.highestUnlockedLevel, levelNumber)
            )
        )
    }

    suspend fun consumeHint(): Boolean {
        val profile = dao.getPlayerProfileOnce() ?: PlayerProfileEntity()
        if (profile.hintsBalance <= 0) {
            dao.upsertPlayerProfile(profile.copy(hintsBalance = 3))
            return true
        }
        dao.upsertPlayerProfile(profile.copy(hintsBalance = profile.hintsBalance - 1))
        return true
    }

    suspend fun saveActiveLevelProgress(
        levelNumber: Int,
        remainingArrowIds: List<Int>,
        remainingSeconds: Int,
        elapsedSeconds: Int,
        livesRemaining: Int
    ) {
        val existing = dao.getLevelProgress(levelNumber) ?: LevelProgressEntity(
            levelNumber = levelNumber,
            isUnlocked = true
        )
        dao.upsertLevelProgress(
            existing.copy(
                savedRemainingArrowIdsCsv = remainingArrowIds.joinToString(","),
                savedRemainingSeconds = remainingSeconds,
                savedElapsedSeconds = elapsedSeconds,
                savedLivesRemaining = livesRemaining,
                hasSavedProgress = remainingArrowIds.isNotEmpty(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearActiveLevelProgress(levelNumber: Int) {
        val existing = dao.getLevelProgress(levelNumber) ?: return
        dao.upsertLevelProgress(
            existing.copy(
                savedRemainingArrowIdsCsv = "",
                savedRemainingSeconds = 0,
                savedElapsedSeconds = 0,
                savedLivesRemaining = 5,
                hasSavedProgress = false
            )
        )
    }

    suspend fun getLevelProgress(levelNumber: Int): LevelProgressEntity? {
        return dao.getLevelProgress(levelNumber)
    }

    suspend fun updateSettings(
        soundEnabled: Boolean? = null,
        vibrationEnabled: Boolean? = null,
        musicEnabled: Boolean? = null,
        voiceEnabled: Boolean? = null,
        selectedThemeId: String? = null,
        themeMode: String? = null,
        arrowTipStyle: String? = null,
        eyeProtectionMode: Boolean? = null
    ) {
        val profile = dao.getPlayerProfileOnce() ?: PlayerProfileEntity()
        dao.upsertPlayerProfile(
            profile.copy(
                soundEnabled = soundEnabled ?: profile.soundEnabled,
                vibrationEnabled = vibrationEnabled ?: profile.vibrationEnabled,
                musicEnabled = musicEnabled ?: profile.musicEnabled,
                voiceEnabled = voiceEnabled ?: profile.voiceEnabled,
                selectedThemeId = selectedThemeId ?: profile.selectedThemeId,
                themeMode = themeMode ?: profile.themeMode,
                arrowTipStyle = arrowTipStyle ?: profile.arrowTipStyle,
                eyeProtectionMode = eyeProtectionMode ?: profile.eyeProtectionMode
            )
        )
    }

    suspend fun clearLockedDifficultyMode() {
        val profile = dao.getPlayerProfileOnce() ?: PlayerProfileEntity()
        dao.upsertPlayerProfile(profile.copy(lockedDifficultyTierName = ""))
    }

    suspend fun executeShopPurchase(
        costPoints: Int,
        nowMs: Long = System.currentTimeMillis(),
        addBonusSecondsToBank: Int = 0,
        addHintsCount: Int = 0,
        newLockedDifficultyTierName: String? = null
    ): ShopPurchaseOutcome {
        val profile = dao.getPlayerProfileOnce() ?: PlayerProfileEntity()
        val recentTimestamps = parseRecentPurchaseTimestamps(profile.purchaseTimestampsCsv, nowMs)

        if (recentTimestamps.size >= MAX_PURCHASES_PER_HOUR) {
            val oldestInWindow = recentTimestamps.minOrNull() ?: nowMs
            val waitSeconds = (((oldestInWindow + ONE_HOUR_MS) - nowMs) / 1000L).toInt().coerceAtLeast(1)
            return ShopPurchaseOutcome.RateLimited(waitSeconds)
        }

        if (profile.totalScore < costPoints) {
            return ShopPurchaseOutcome.InsufficientPoints(
                required = costPoints,
                available = profile.totalScore
            )
        }

        val updatedTimestamps = (recentTimestamps + nowMs).joinToString(",")
        val updatedProfile = profile.copy(
            totalScore = (profile.totalScore - costPoints).coerceAtLeast(0),
            hintsBalance = profile.hintsBalance + addHintsCount.coerceAtLeast(0),
            totalPurchasesCount = profile.totalPurchasesCount + 1,
            purchaseTimestampsCsv = updatedTimestamps,
            bonusTimeSecondsBank = profile.bonusTimeSecondsBank + addBonusSecondsToBank,
            lockedDifficultyTierName = newLockedDifficultyTierName ?: profile.lockedDifficultyTierName
        )
        dao.upsertPlayerProfile(updatedProfile)
        return ShopPurchaseOutcome.Success(
            remainingScore = updatedProfile.totalScore,
            purchasesUsedInHour = recentTimestamps.size + 1,
            newTotalPurchasesCount = updatedProfile.totalPurchasesCount
        )
    }

    suspend fun purchaseInLevelTool(cost: Int): Boolean {
        val profile = dao.getPlayerProfileOnce() ?: return false
        if (profile.totalScore < cost) return false
        dao.upsertPlayerProfile(
            profile.copy(
                totalScore = (profile.totalScore - cost).coerceAtLeast(0)
            )
        )
        return true
    }

    suspend fun unlockTierFirstLevel(tierStartLevel: Int) {
        val existing = dao.getLevelProgress(tierStartLevel)
        if (existing == null || !existing.isUnlocked) {
            dao.upsertLevelProgress(
                LevelProgressEntity(
                    levelNumber = tierStartLevel,
                    isUnlocked = true,
                    isCompleted = existing?.isCompleted ?: false
                )
            )
        }
    }

    companion object {
        const val MAX_PURCHASES_PER_HOUR = 5
        const val ONE_HOUR_MS = 3_600_000L

        /**
         * Dynamic escalating cost for purchasing immediate unlock of Hint, Path Inspect & Auto-Solve
         * (cancelling the 3-minute cooldown timer + adding 2 extra hints):
         * Starts at 80 points and increases progressively with each purchase completed by the user.
         */
        fun computeUnlockToolsCost(totalPurchasesCount: Int): Int {
            return 50 + (totalPurchasesCount.coerceAtLeast(0) * 25)
        }

        /**
         * Dynamic escalating cost for purchasing +5 Extra Attempts (شراء 5 محاولات إضافية):
         * Starts at 40 points and increases progressively with each purchase completed by the user.
         */
        fun computeExtraAttemptsCost(totalPurchasesCount: Int): Int {
            return 40 + (totalPurchasesCount.coerceAtLeast(0) * 20)
        }

        /**
         * Dynamic escalating cost for purchasing +60s Extra Time:
         * Starts at 35 points and increases progressively with each purchase completed by the user.
         */
        fun computeExtraTimeCost(totalPurchasesCount: Int): Int {
            return 35 + (totalPurchasesCount.coerceAtLeast(0) * 15)
        }

        /**
         * Dynamic escalating cost for purchasing "Stay in Specific Mode" (تثبيت وضع معين):
         * Starts at 75 points and increases progressively with each purchase completed by the user.
         */
        fun computeStayInModeCost(totalPurchasesCount: Int): Int {
            return 75 + (totalPurchasesCount.coerceAtLeast(0) * 30)
        }

        fun parseRecentPurchaseTimestamps(csv: String, nowMs: Long = System.currentTimeMillis()): List<Long> {
            if (csv.isBlank()) return emptyList()
            return csv.split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { nowMs - it in 0L..ONE_HOUR_MS }
        }
    }
}

sealed class ShopPurchaseOutcome {
    data class Success(
        val remainingScore: Int,
        val purchasesUsedInHour: Int,
        val newTotalPurchasesCount: Int
    ) : ShopPurchaseOutcome()

    data class InsufficientPoints(
        val required: Int,
        val available: Int
    ) : ShopPurchaseOutcome()

    data class RateLimited(
        val retryAfterSeconds: Int
    ) : ShopPurchaseOutcome()
}
