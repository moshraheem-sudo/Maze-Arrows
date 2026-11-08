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
        arabicLabel = "محترف",
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
         * across levels with heightened challenge, while stepping up to a strictly harder mode
         * whenever [escalationSteps] > 0 (on retry / exhausted attempts).
         */
        fun dynamicForLevel(levelNumber: Int, escalationSteps: Int = 0): DifficultyTier {
            val baseTierIndex = when {
                levelNumber == 1 -> 1 // Starts right away with dense Medium-grade challenge
                levelNumber == 2 -> 2 // HARD
                levelNumber == 3 -> 3 // EXPERT challenge wave
                levelNumber == 4 -> 1 // Dense breather wave
                levelNumber == 5 -> 2 // HARD
                levelNumber == 6 -> 3 // EXPERT
                levelNumber == 7 -> 4 // PRO master wave
                levelNumber == 8 -> 2 // HARD wave
                else -> {
                    // Dynamic 6-level high-intensity wave cycle
                    val cyclePos = (levelNumber - 9) % 6
                    val progressionBoost = ((levelNumber - 1) / 25).coerceIn(0, 2)
                    when (cyclePos) {
                        0 -> (1 + progressionBoost).coerceAtMost(3)
                        1 -> (2 + progressionBoost).coerceAtMost(4)
                        2 -> (3 + progressionBoost).coerceAtMost(4)
                        3 -> (2 + progressionBoost).coerceAtMost(3)
                        4 -> (4).coerceAtMost(4)
                        else -> (3 + progressionBoost).coerceAtMost(4)
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

enum class ArrowTipStyle(
    val id: String,
    val arabicName: String,
    val descriptionAr: String
) {
    CLASSIC_TRIANGLE("CLASSIC_TRIANGLE", "مثلث كلاسيكي", "سهم عريض متوازن ومريح للرؤية"),
    STEALTH_CHEVRON("STEALTH_CHEVRON", "شفرة الشبح الحادة", "سهم انسيابي بحواف حادة مقعرة"),
    DIAMOND_HEAD("DIAMOND_HEAD", "رأس الماسة", "رأس ماسي أنيق ومحدد بدقة"),
    ROUNDED_SHIELD("ROUNDED_SHIELD", "درع انسيابي", "سهم ناعم مقوّس مريح للعين"),
    HARPOON_BARB("HARPOON_BARB", "خطاف بارز", "رأس رمحي بجناحين بارزين");

    companion object {
        fun fromId(id: String): ArrowTipStyle =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CLASSIC_TRIANGLE
    }
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

data class BoardThemePalette(
    val id: String,
    val arabicName: String,
    val boardBackgroundHex: Long,
    val cardSurfaceHex: Long,
    val dotColorHex: Long,
    val primaryArrowHex: Long,
    val secondaryArrowHex: Long,
    val highlightArrowHex: Long,
    val blockedArrowHex: Long,
    val isDark: Boolean = false
) {
    companion object {
        val AMAZE_CLASSIC = BoardThemePalette(
            id = "amaze_classic",
            arabicName = "النمط الكلاسيكي الافتراضي",
            boardBackgroundHex = 0xFFFAF4EB,
            cardSurfaceHex = 0xFFFFFBF4,
            dotColorHex = 0xFFC2B29D,
            primaryArrowHex = 0xFF332211,
            secondaryArrowHex = 0xFF332211,
            highlightArrowHex = 0xFFF5A623,
            blockedArrowHex = 0xFFE53935,
            isDark = false
        )

        val OBSIDIAN_NIGHT = BoardThemePalette(
            id = "obsidian_night",
            arabicName = "ليلي فاخر نيون",
            boardBackgroundHex = 0xFF10141D,
            cardSurfaceHex = 0xFF1C2331,
            dotColorHex = 0xFF64748B,
            primaryArrowHex = 0xFFFDF8F0,
            secondaryArrowHex = 0xFF9EC5FE,
            highlightArrowHex = 0xFFFFCA28,
            blockedArrowHex = 0xFFFF5252,
            isDark = true
        )

        val EMERALD_ZEN = BoardThemePalette(
            id = "emerald_zen",
            arabicName = "زمردي هادئ",
            boardBackgroundHex = 0xFFF1F7F3,
            cardSurfaceHex = 0xFFFFFFFF,
            dotColorHex = 0xFF52796F,
            primaryArrowHex = 0xFF1B4332,
            secondaryArrowHex = 0xFF2D6A4F,
            highlightArrowHex = 0xFFF4A261,
            blockedArrowHex = 0xFFD62828,
            isDark = false
        )

        val SANDSTONE_GOLD = BoardThemePalette(
            id = "sandstone_gold",
            arabicName = "رمال الصحراء الذهبية",
            boardBackgroundHex = 0xFFF6ECE0,
            cardSurfaceHex = 0xFFFCF7F1,
            dotColorHex = 0xFF8D6E63,
            primaryArrowHex = 0xFF3E2723,
            secondaryArrowHex = 0xFF5D4037,
            highlightArrowHex = 0xFFFF8F00,
            blockedArrowHex = 0xFFC62828,
            isDark = false
        )

        val MONOCHROME_UNIFIED = BoardThemePalette(
            id = "monochrome_unified",
            arabicName = "لون موحد أنيق",
            boardBackgroundHex = 0xFFF8FAFC,
            cardSurfaceHex = 0xFFFFFFFF,
            dotColorHex = 0xFF94A3B8,
            primaryArrowHex = 0xFF1E293B,
            secondaryArrowHex = 0xFF1E293B,
            highlightArrowHex = 0xFFF59E0B,
            blockedArrowHex = 0xFFDC2626,
            isDark = false
        )

        val OCEAN_SAPPHIRE = BoardThemePalette(
            id = "ocean_sapphire",
            arabicName = "أزرق ياقوتي بحري",
            boardBackgroundHex = 0xFFF0F5FA,
            cardSurfaceHex = 0xFFFFFFFF,
            dotColorHex = 0xFF4A6984,
            primaryArrowHex = 0xFF0F3460,
            secondaryArrowHex = 0xFF16213E,
            highlightArrowHex = 0xFF00ADB5,
            blockedArrowHex = 0xFFE94560,
            isDark = false
        )

        val entries: List<BoardThemePalette> = listOf(
            AMAZE_CLASSIC,
            OBSIDIAN_NIGHT,
            EMERALD_ZEN,
            SANDSTONE_GOLD,
            MONOCHROME_UNIFIED,
            OCEAN_SAPPHIRE
        )

        fun fromId(id: String): BoardThemePalette =
            entries.firstOrNull { it.id == id } ?: AMAZE_CLASSIC

        fun resolveForThemeMode(
            selectedThemeId: String,
            themeMode: AppThemeMode,
            isSystemDark: Boolean
        ): BoardThemePalette {
            val base = fromId(selectedThemeId)
            val isAppDark = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemDark
            }

            return if (isAppDark) {
                when (base.id) {
                    MONOCHROME_UNIFIED.id -> base.copy(
                        isDark = true,
                        boardBackgroundHex = 0xFF10141D,
                        cardSurfaceHex = 0xFF1C2331,
                        primaryArrowHex = 0xFFF8FAFC,
                        secondaryArrowHex = 0xFFF8FAFC,
                        dotColorHex = 0xFF94A3B8,
                        highlightArrowHex = 0xFFFFD166,
                        blockedArrowHex = 0xFFFF5252
                    )
                    AMAZE_CLASSIC.id -> base.copy(
                        isDark = true,
                        boardBackgroundHex = 0xFF10141D,
                        cardSurfaceHex = 0xFF1C2331,
                        primaryArrowHex = 0xFFE2E8F0,
                        secondaryArrowHex = 0xFFE2E8F0,
                        dotColorHex = 0xFF64748B,
                        highlightArrowHex = 0xFFFFD166,
                        blockedArrowHex = 0xFFFF5252
                    )
                    EMERALD_ZEN.id -> base.copy(
                        isDark = true,
                        boardBackgroundHex = 0xFF10141D,
                        cardSurfaceHex = 0xFF1C2331,
                        primaryArrowHex = 0xFF74C69D,
                        secondaryArrowHex = 0xFF52B788,
                        dotColorHex = 0xFF52796F,
                        highlightArrowHex = 0xFFFFB703,
                        blockedArrowHex = 0xFFFF5252
                    )
                    SANDSTONE_GOLD.id -> base.copy(
                        isDark = true,
                        boardBackgroundHex = 0xFF10141D,
                        cardSurfaceHex = 0xFF1C2331,
                        primaryArrowHex = 0xFFFFD54F,
                        secondaryArrowHex = 0xFFFFB300,
                        dotColorHex = 0xFF8D6E63,
                        highlightArrowHex = 0xFFFF6F00,
                        blockedArrowHex = 0xFFFF5252
                    )
                    OCEAN_SAPPHIRE.id -> base.copy(
                        isDark = true,
                        boardBackgroundHex = 0xFF10141D,
                        cardSurfaceHex = 0xFF1C2331,
                        primaryArrowHex = 0xFF38BDF8,
                        secondaryArrowHex = 0xFF60A5FA,
                        dotColorHex = 0xFF3B82F6,
                        highlightArrowHex = 0xFF22D3EE,
                        blockedArrowHex = 0xFFFF5252
                    )
                    else -> base.copy(
                        isDark = true,
                        boardBackgroundHex = 0xFF10141D,
                        cardSurfaceHex = 0xFF1C2331,
                        primaryArrowHex = 0xFFFDF8F0,
                        secondaryArrowHex = 0xFF9EC5FE,
                        dotColorHex = 0xFF64748B,
                        highlightArrowHex = 0xFFFFCA28,
                        blockedArrowHex = 0xFFFF5252
                    )
                }
            } else {
                when (base.id) {
                    MONOCHROME_UNIFIED.id -> base.copy(
                        isDark = false,
                        boardBackgroundHex = 0xFFFBF5EA,
                        cardSurfaceHex = 0xFFFFFBF4,
                        primaryArrowHex = 0xFF1E293B,
                        secondaryArrowHex = 0xFF1E293B,
                        dotColorHex = 0xFF94A3B8,
                        highlightArrowHex = 0xFFF59E0B,
                        blockedArrowHex = 0xFFDC2626
                    )
                    OBSIDIAN_NIGHT.id -> base.copy(
                        isDark = false,
                        boardBackgroundHex = 0xFFFBF5EA,
                        cardSurfaceHex = 0xFFFFFBF4,
                        primaryArrowHex = 0xFF1E293B,
                        secondaryArrowHex = 0xFF334155,
                        dotColorHex = 0xFF64748B,
                        highlightArrowHex = 0xFF0284C7,
                        blockedArrowHex = 0xFFDC2626
                    )
                    AMAZE_CLASSIC.id -> base.copy(
                        isDark = false,
                        boardBackgroundHex = 0xFFFAF4EB,
                        cardSurfaceHex = 0xFFFFFBF4,
                        primaryArrowHex = 0xFF332211,
                        secondaryArrowHex = 0xFF422E1A,
                        highlightArrowHex = 0xFFF5A623,
                        dotColorHex = 0xFFC2B29D,
                        blockedArrowHex = 0xFFE53935
                    )
                    else -> base.copy(
                        isDark = false,
                        boardBackgroundHex = 0xFFFBF5EA,
                        cardSurfaceHex = 0xFFFFFBF4
                    )
                }
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
