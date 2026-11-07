package com.example.engine

import kotlin.math.floor

enum class Direction(val dx: Int, val dy: Int, val symbol: String) {
    UP(0, -1, "↑"),
    DOWN(0, 1, "↓"),
    LEFT(-1, 0, "←"),
    RIGHT(1, 0, "→");

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }

    fun turnClockwise(): Direction = when (this) {
        UP -> RIGHT
        RIGHT -> DOWN
        DOWN -> LEFT
        LEFT -> UP
    }

    fun turnCounterClockwise(): Direction = when (this) {
        UP -> LEFT
        LEFT -> DOWN
        DOWN -> RIGHT
        RIGHT -> UP
    }
}

data class GridPoint(val x: Int, val y: Int) {
    fun step(dir: Direction, count: Int = 1): GridPoint =
        GridPoint(x + dir.dx * count, y + dir.dy * count)
}

enum class ArrowStyle(val arabicName: String) {
    SPIRAL("حلزوني متداخل"),
    SERPENTINE("متعرج ثعباني"),
    COMB_U("مشط مزدوج U"),
    BRACKET("قوس متشابك"),
    HOOK("خطاف زاوي"),
    STRAIGHT("مستقيم")
}

enum class LevelAlgorithmArchetype(
    val arabicTitle: String,
    val arabicDescription: String
) {
    SPIRAL_CITADEL(
        arabicTitle = "القلاع الحلزونية",
        arabicDescription = "دوامات حلزونية متقاربة تحيط بها خطافات متشابكة"
    ),
    SERPENTINE_LABYRINTH(
        arabicTitle = "المتاهة الثعبانية",
        arabicDescription = "مسارات متموجة ومتوازية تلتف بدون فراغات"
    ),
    INTERLOCKING_COMBS(
        arabicTitle = "الأمشاط المتداخلة",
        arabicDescription = "صفوف أقواس متراصة تقفل مسارات الأسهم المتقاطعة"
    ),
    VORTEX_CONVERGENCE(
        arabicTitle = "البؤر المتعددة",
        arabicDescription = "توليد كثيف يملأ كافة الزوايا بأسهم متقاربة"
    ),
    HYBRID_GRANDMASTER(
        arabicTitle = "المزيج المتقدم",
        arabicDescription = "أقصى كثافة امتلاء تجمع الحلزونات والممرات الدقيقة"
    )
}

data class WindingArrow(
    val id: Int,
    val cells: List<GridPoint>,
    val headDirection: Direction,
    val style: ArrowStyle = ArrowStyle.HOOK,
    val colorAccentIndex: Int = 0,
    val hasSparkleBadge: Boolean = false
) {
    val head: GridPoint
        get() = cells.last()

    val tail: GridPoint
        get() = cells.first()

    val length: Int
        get() = cells.size

    fun getInterpolatedPolyline(
        slideSteps: Float,
        extraExitSteps: Int = 42
    ): List<Pair<Float, Float>> {
        if (cells.isEmpty()) return emptyList()
        val s = slideSteps.coerceAtLeast(0f)
        if (cells.size == 1) {
            val c = cells[0]
            val hx = c.x + headDirection.dx * s
            val hy = c.y + headDirection.dy * s
            val tx = hx - headDirection.dx * 0.55f
            val ty = hy - headDirection.dy * 0.55f
            return listOf(tx to ty, hx to hy)
        }

        val totalExtension = extraExitSteps + cells.size + 8
        val trackSize = cells.size + totalExtension
        fun pointAt(idx: Int): Pair<Float, Float> {
            return if (idx < cells.size) {
                cells[idx].x.toFloat() to cells[idx].y.toFloat()
            } else {
                val extra = idx - (cells.size - 1)
                val h = cells.last()
                (h.x + headDirection.dx * extra).toFloat() to (h.y + headDirection.dy * extra).toFloat()
            }
        }

        val span = (cells.size - 1).toFloat()
        val tailPos = s.coerceAtMost((trackSize - 2).toFloat())
        val headPos = (s + span).coerceAtMost((trackSize - 1).toFloat())

        val tFloor = floor(tailPos).toInt().coerceIn(0, trackSize - 2)
        val tFrac = (tailPos - tFloor).coerceIn(0f, 1f)

        val hFloor = floor(headPos).toInt().coerceIn(0, trackSize - 2)
        val hFrac = (headPos - hFloor).coerceIn(0f, 1f)

        val result = ArrayList<Pair<Float, Float>>(cells.size + 2)

        val pT0 = pointAt(tFloor)
        val pT1 = pointAt(tFloor + 1)
        val startX = pT0.first + (pT1.first - pT0.first) * tFrac
        val startY = pT0.second + (pT1.second - pT0.second) * tFrac
        result.add(startX to startY)

        for (idx in (tFloor + 1)..hFloor) {
            result.add(pointAt(idx))
        }

        val pH0 = pointAt(hFloor)
        val pH1 = pointAt(hFloor + 1)
        val endX = pH0.first + (pH1.first - pH0.first) * hFrac
        val endY = pH0.second + (pH1.second - pH0.second) * hFrac
        val lastAdded = result.last()
        if (kotlin.math.abs(lastAdded.first - endX) > 0.001f ||
            kotlin.math.abs(lastAdded.second - endY) > 0.001f
        ) {
            result.add(endX to endY)
        }

        return result
    }
}

enum class DifficultyTier(
    val tierIndex: Int,
    val arabicLabel: String,
    val englishLabel: String,
    val descriptionAr: String,
    val levelRange: IntRange,
    val badgeColorHex: Long
) {
    EASY(
        tierIndex = 0,
        arabicLabel = "سهل",
        englishLabel = "Easy",
        descriptionAr = "المراحل 1–10: لوحات متقاربة بدون فراغات لبداية متدرجة",
        levelRange = 1..10,
        badgeColorHex = 0xFF2E7D32
    ),
    MEDIUM(
        tierIndex = 1,
        arabicLabel = "متوسط",
        englishLabel = "Medium",
        descriptionAr = "المراحل 11–30: زيادة تلقائية في عدد الأسهم وتشابك المسارات",
        levelRange = 11..30,
        badgeColorHex = 0xFF1565C0
    ),
    HARD(
        tierIndex = 2,
        arabicLabel = "صعب",
        englishLabel = "Hard",
        descriptionAr = "المراحل 31–80: كثافة عالية جداً مع قلاع حلزونية وأمشاط متداخلة",
        levelRange = 31..80,
        badgeColorHex = 0xFF7B1FA2
    ),
    EXPERT(
        tierIndex = 3,
        arabicLabel = "خبير",
        englishLabel = "Expert",
        descriptionAr = "المراحل 81–200: متاهات واسعة ممتلئة بالكامل تتطلب تخطيطاً عميقاً",
        levelRange = 81..200,
        badgeColorHex = 0xFFE65100
    ),
    PRO(
        tierIndex = 4,
        arabicLabel = "احترافي",
        englishLabel = "Pro Master",
        descriptionAr = "المراحل 201–500: أقصى درجات الامتلاء والتعقيد الملحمي للمحترفين",
        levelRange = 201..500,
        badgeColorHex = 0xFFC62828
    );

    companion object {
        fun forLevel(levelNumber: Int): DifficultyTier {
            return dynamicForLevel(levelNumber, escalationSteps = 0)
        }

        /**
         * Automatically alternates and varies between Easy, Medium, Hard, Expert, and Pro modes
         * across levels to keep the player engaged (flow-state pacing), while stepping up to a
         * strictly harder mode whenever [escalationSteps] > 0 (on retry / exhausted attempts).
         */
        fun dynamicForLevel(levelNumber: Int, escalationSteps: Int = 0): DifficultyTier {
            val baseTierIndex = when {
                levelNumber == 1 -> 0 // EASY
                levelNumber == 2 -> 1 // MEDIUM
                levelNumber == 3 -> 2 // HARD challenge wave
                levelNumber == 4 -> 0 // EASY breather wave
                levelNumber == 5 -> 1 // MEDIUM
                levelNumber == 6 -> 2 // HARD
                levelNumber == 7 -> 3 // EXPERT spike
                levelNumber == 8 -> 1 // MEDIUM breather
                else -> {
                    // Dynamic 6-level wave cycle that alternates easy/medium/hard/expert/pro
                    val cyclePos = (levelNumber - 9) % 6
                    val progressionBoost = ((levelNumber - 1) / 35).coerceIn(0, 2)
                    when (cyclePos) {
                        0 -> (0 + progressionBoost).coerceAtMost(2) // Refreshing Easy/Medium wave
                        1 -> (1 + progressionBoost).coerceAtMost(3) // Medium/Hard wave
                        2 -> (2 + progressionBoost).coerceAtMost(4) // Hard/Expert wave
                        3 -> (1 + progressionBoost).coerceAtMost(2) // Smooth Medium breather
                        4 -> (3 + progressionBoost).coerceAtMost(4) // Expert/Pro challenge
                        else -> (2 + progressionBoost).coerceAtMost(4) // Hard/Pro wave
                    }
                }
            }
            val escalatedIndex = (baseTierIndex + escalationSteps).coerceIn(0, entries.lastIndex)
            return entries[escalatedIndex]
        }

        fun progressiveSubtitle(levelNumber: Int, escalationSteps: Int = 0): String {
            val tier = dynamicForLevel(levelNumber, escalationSteps)
            return if (escalationSteps > 0) {
                "${tier.arabicLabel} (+$escalationSteps)"
            } else {
                tier.arabicLabel
            }
        }
    }
}

enum class BoardShapeMask(val arabicName: String) {
    FULL_MATRIX("شبكة كاملة"),
    CLASSIC_TOWER("البرج"),
    TWIN_PILLARS("الأعمدة"),
    STEPPED_CITADEL("القلعة"),
    LABYRINTH_CORE("المتاهة"),
    HOURGLASS("الساعة الرملية")
}

enum class AppThemeMode(
    val id: String,
    val arabicLabel: String
) {
    LIGHT("LIGHT", "الثيم الفاتح"),
    DARK("DARK", "الثيم الداكن"),
    SYSTEM("SYSTEM", "الافتراضي للنظام");

    companion object {
        fun fromId(id: String): AppThemeMode =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
    }
}

enum class BoardThemePalette(
    val id: String,
    val arabicName: String,
    val boardBackgroundHex: Long,
    val cardSurfaceHex: Long,
    val dotColorHex: Long,
    val primaryArrowHex: Long,
    val secondaryArrowHex: Long,
    val highlightArrowHex: Long,
    val blockedArrowHex: Long,
    val isDark: Boolean
) {
    AMAZE_CLASSIC(
        id = "amaze_classic",
        arabicName = "كلاسيكي دافئ (Amaze GO)",
        boardBackgroundHex = 0xFFFBF5EA,
        cardSurfaceHex = 0xFFFFFBF4,
        dotColorHex = 0xFFD7C6B0,
        primaryArrowHex = 0xFF53351A,
        secondaryArrowHex = 0xFF6B4624,
        highlightArrowHex = 0xFFF3A833,
        blockedArrowHex = 0xFFE53935,
        isDark = false
    ),
    OBSIDIAN_NIGHT(
        id = "obsidian_night",
        arabicName = "ليلي فاخر (Obsidian)",
        boardBackgroundHex = 0xFF10141D,
        cardSurfaceHex = 0xFF1C2331,
        dotColorHex = 0xFF3E4C66,
        primaryArrowHex = 0xFFFDF8F0,
        secondaryArrowHex = 0xFF9EC5FE,
        highlightArrowHex = 0xFFFFCA28,
        blockedArrowHex = 0xFFFF5252,
        isDark = true
    ),
    EMERALD_ZEN(
        id = "emerald_zen",
        arabicName = "زمردي هادئ (Zen)",
        boardBackgroundHex = 0xFFF1F7F3,
        cardSurfaceHex = 0xFFFFFFFF,
        dotColorHex = 0xFFB8CFC1,
        primaryArrowHex = 0xFF1B4332,
        secondaryArrowHex = 0xFF2D6A4F,
        highlightArrowHex = 0xFFF4A261,
        blockedArrowHex = 0xFFD62828,
        isDark = false
    ),
    SANDSTONE_GOLD(
        id = "sandstone_gold",
        arabicName = "رمال الصحراء الذهبية",
        boardBackgroundHex = 0xFFF6ECE0,
        cardSurfaceHex = 0xFFFCF7F1,
        dotColorHex = 0xFFCEB89E,
        primaryArrowHex = 0xFF3E2723,
        secondaryArrowHex = 0xFF5D4037,
        highlightArrowHex = 0xFFFF8F00,
        blockedArrowHex = 0xFFC62828,
        isDark = false
    );

    companion object {
        fun fromId(id: String): BoardThemePalette =
            entries.firstOrNull { it.id == id } ?: AMAZE_CLASSIC

        fun resolveForThemeMode(
            selectedThemeId: String,
            themeMode: AppThemeMode,
            isSystemDark: Boolean
        ): BoardThemePalette {
            val base = fromId(selectedThemeId)
            val shouldUseDark = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemDark
            }
            return if (shouldUseDark) {
                OBSIDIAN_NIGHT
            } else {
                if (base.isDark) AMAZE_CLASSIC else base
            }
        }
    }
}

sealed class MoveCheckResult {
    data class Clear(
        val arrowId: Int,
        val stepsToEdge: Int
    ) : MoveCheckResult()

    data class Blocked(
        val arrowId: Int,
        val blockerArrowId: Int,
        val collisionPoint: GridPoint,
        val clearStepsBeforeCollision: Int
    ) : MoveCheckResult()
}

data class PuzzleLevel(
    val levelNumber: Int,
    val isCustomGenerated: Boolean = false,
    val difficulty: DifficultyTier,
    val shapeMask: BoardShapeMask,
    val algorithmArchetype: LevelAlgorithmArchetype,
    val gridWidth: Int,
    val gridHeight: Int,
    val activeMaskCells: Set<GridPoint>,
    val arrows: List<WindingArrow>,
    val solverMetrics: SolverMetrics,
    val fillDensityPercent: Int,
    val playerDnaCode: String,
    val seed: Long
)
