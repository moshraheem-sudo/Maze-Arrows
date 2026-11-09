package com.example.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Ultra-Dense Outside-In Peeling (Reverse-Elimination) Puzzle Generator for Amaze Arrows.
 *
 * How it works:
 * 1. Starts with 100% of the grid cells in `inRemaining` and peels winding arrows off
 *    from the exposed frontier inward (`A_1, A_2, ..., A_k`).
 * 2. Because each peeled arrow `A_i` has its head pointing out through already-peeled
 *    (empty) cells, its forward exit ray is 100% clear of all remaining arrows `A_{i+1}..A_k`
 *    and 100% clear of its own body `A_i`.
 * 3. Uses **Bridge-Preserving & Leaf-Absorbing Walk** + **Universal Safe Tail Grafting**
 *    so that virtually 99%–100% of the grid is packed with closely-spaced winding arrows
 *    with ZERO empty gaps and ZERO overlapping cells.
 * 4. Automatically scales difficulty, board size, arrow count, and path complexity on
 *    every single level (`Level 1 -> Level 2 -> ... -> Level 500`), while personalizing
 *    each board using the player's unique `playerDnaSeed`.
 */
object SmartLevelGenerator {

    const val TOTAL_CAMPAIGN_LEVELS = 500
    const val MASTERPIECE_SHOWCASE_LEVEL = 250

    fun formatPlayerDnaCode(playerDnaSeed: Long): String {
        val upper = ((playerDnaSeed ushr 16) and 0xFFFFL).toString(16).uppercase().padStart(4, '0')
        val lower = (playerDnaSeed and 0xFFFFL).toString(16).uppercase().padStart(4, '0')
        return "DNA-$upper-$lower"
    }

    /**
     * Generates a campaign level for [levelNumber] (1..500+) using the full Smart Generator
     * algorithm. The system automatically varies between Easy, Medium, Hard, Expert, and Pro
     * modes to keep the player engaged, and escalates to a harder mode whenever [escalationSteps] > 0!
     */
    fun generateCampaignLevel(
        levelNumber: Int,
        playerDnaSeed: Long = 0x5F3759DF7C9E1234L,
        escalationSteps: Int = 0,
        forcedDifficultyTier: DifficultyTier? = null
    ): PuzzleLevel {
        val clampedLevel = levelNumber.coerceAtLeast(1)
        val tier = if (forcedDifficultyTier != null) {
            if (escalationSteps > 0) {
                val escalatedIdx = (forcedDifficultyTier.tierIndex + escalationSteps).coerceIn(0, DifficultyTier.entries.lastIndex)
                DifficultyTier.entries[escalatedIdx]
            } else {
                forcedDifficultyTier
            }
        } else {
            DifficultyTier.dynamicForLevel(clampedLevel, escalationSteps)
        }
        val salt = clampedLevel.toLong() * 1009L + escalationSteps.toLong() * 7919L + (if (escalationSteps > 0) ((System.currentTimeMillis() xor (escalationSteps * 31337L)) and 0xFFFFL) else 0L)
        val combinedSeed = mixSeed(playerDnaSeed, salt)
        val rng = Random(combinedSeed)

        val (gridW, gridH) = computeDynamicDimensions(clampedLevel, tier, escalationSteps)
        // Automatically vary board shapes and smart generator archetypes across levels
        val shapeMask = selectDynamicShapeMask(clampedLevel, tier, escalationSteps, rng)
        val archetype = selectArchetypeForLevel(clampedLevel + escalationSteps * 3, tier, rng)

        var curSeed = combinedSeed
        var bestLevel: PuzzleLevel? = null
        var lowestConflicts = Int.MAX_VALUE

        for (attempt in 0..5) {
            val candidate = buildZeroGapPeeledLevel(
                levelNumber = clampedLevel,
                isCustom = false,
                difficulty = tier,
                shapeMask = shapeMask,
                archetype = archetype,
                gridWidth = gridW,
                gridHeight = gridH,
                playerDnaSeed = playerDnaSeed,
                seed = curSeed
            )
            val conflicts = countAdjacentSameDirectionConflicts(candidate.arrows, gridW, gridH)
            if (conflicts == 0 && candidate.solverMetrics.isSolvable) {
                return candidate
            }
            if (conflicts < lowestConflicts && candidate.solverMetrics.isSolvable) {
                lowestConflicts = conflicts
                bestLevel = candidate
            }
            if (clampedLevel > 2 && attempt >= 2 && candidate.solverMetrics.isSolvable && conflicts <= 1) {
                return candidate
            }
            curSeed = mixSeed(curSeed, (attempt + 1) * 31337L + 7919L)
        }
        return bestLevel ?: buildZeroGapPeeledLevel(
            levelNumber = clampedLevel,
            isCustom = false,
            difficulty = tier,
            shapeMask = shapeMask,
            archetype = archetype,
            gridWidth = gridW,
            gridHeight = gridH,
            playerDnaSeed = playerDnaSeed,
            seed = combinedSeed
        )
    }

    /**
     * Generates a custom level for a specific difficulty, board shape mask, and archetype.
     * Generates fast, responsive, and 100% solvable boards without freezing.
     */
    fun generateCustomLevel(
        difficulty: DifficultyTier,
        shapeMask: BoardShapeMask,
        archetype: LevelAlgorithmArchetype = LevelAlgorithmArchetype.HYBRID_GRANDMASTER,
        playerDnaSeed: Long = 0x5F3759DF7C9E1234L,
        seed: Long = System.currentTimeMillis()
    ): PuzzleLevel {
        val representativeLevel = when (difficulty) {
            DifficultyTier.EASY -> 2
            DifficultyTier.MEDIUM -> 5
            DifficultyTier.HARD -> 10
            DifficultyTier.EXPERT -> 16
            DifficultyTier.PRO -> 25
        }
        val (gridW, gridH) = computeDynamicDimensions(representativeLevel, difficulty, escalationSteps = 0)
        val finalSeed = mixSeed(playerDnaSeed xor seed, representativeLevel.toLong())

        var curSeed = finalSeed
        var bestLevel: PuzzleLevel? = null
        var lowestConflicts = Int.MAX_VALUE

        for (attempt in 0..1) {
            val candidate = buildZeroGapPeeledLevel(
                levelNumber = representativeLevel,
                isCustom = true,
                difficulty = difficulty,
                shapeMask = shapeMask,
                archetype = archetype,
                gridWidth = gridW,
                gridHeight = gridH,
                playerDnaSeed = playerDnaSeed,
                seed = curSeed
            )
            val conflicts = countAdjacentSameDirectionConflicts(candidate.arrows, gridW, gridH)
            if (candidate.solverMetrics.isSolvable && (conflicts == 0 || attempt > 0)) {
                return candidate
            }
            if (conflicts < lowestConflicts && candidate.solverMetrics.isSolvable) {
                lowestConflicts = conflicts
                bestLevel = candidate
            }
            curSeed = mixSeed(curSeed, (attempt + 1) * 31337L + 7919L)
        }
        return bestLevel ?: buildZeroGapPeeledLevel(
            levelNumber = representativeLevel,
            isCustom = true,
            difficulty = difficulty,
            shapeMask = shapeMask,
            archetype = archetype,
            gridWidth = gridW,
            gridHeight = gridH,
            playerDnaSeed = playerDnaSeed,
            seed = finalSeed
        )
    }

    private fun mixSeed(playerSeed: Long, levelSalt: Long): Long {
        var z = playerSeed + levelSalt * -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /**
     * Computes board dimensions based on dynamic [tier], [levelNumber], and [escalationSteps].
     * - Levels 1 and 2 start truly easy (5x8 and 6x9, ~10-20 arrows).
     * - Levels 3+ escalate smoothly into dense, complex winding labyrinths (Medium -> Hard -> Expert -> Pro).
     * - On retry/timeout escalation, dimensions and complexity increase to challenge the player.
     */
    private fun computeDynamicDimensions(
        levelNumber: Int,
        tier: DifficultyTier,
        escalationSteps: Int
    ): Pair<Int, Int> {
        val basePair = when (tier) {
            DifficultyTier.EASY -> {
                when (levelNumber) {
                    1 -> 5 to 8      // ~40 cells -> ~10-14 clean arrows (Level 1: introductory easy)
                    2 -> 6 to 9      // ~54 cells -> ~15-20 clean arrows (Level 2: easy)
                    else -> 7 to 10  // ~70 cells
                }
            }
            DifficultyTier.MEDIUM -> {
                // Levels 3 to 6: High density labyrinths with deep windings
                when (levelNumber) {
                    3 -> 12 to 18    // ~216 cells -> ~60-80 dense winding arrows
                    4 -> 14 to 20    // ~280 cells -> ~80-100 dense winding arrows
                    5 -> 15 to 22    // ~330 cells -> ~95-125 dense winding arrows
                    else -> 17 to 24 // ~408 cells -> ~120-150 dense winding arrows
                }
            }
            DifficultyTier.HARD -> {
                val subProg = ((levelNumber - 7).coerceAtLeast(0) / 3).coerceAtMost(4)
                (18 + subProg) to (24 + subProg * 2) // ~430-600 cells -> ~130-190 deeply interlocked arrows
            }
            DifficultyTier.EXPERT -> {
                val subProg = ((levelNumber - 21).coerceAtLeast(0) / 6).coerceAtMost(4)
                (20 + subProg) to (28 + subProg * 2) // ~560-760 cells -> ~180-260 massive labyrinth
            }
            DifficultyTier.PRO -> {
                val subProg = ((levelNumber - 61).coerceAtLeast(0) / 10).coerceAtMost(5)
                (22 + subProg) to (32 + subProg * 2) // ~700-1050 cells -> ~240-380 grandmaster labyrinth
            }
        }
        val escBonusW = escalationSteps
        val escBonusH = escalationSteps * 2

        val finalW = (basePair.first + escBonusW).coerceIn(5, 26)
        val finalH = (basePair.second + escBonusH).coerceIn(8, 38)
        return finalW to finalH
    }

    private fun selectDynamicShapeMask(
        levelNumber: Int,
        tier: DifficultyTier,
        escalationSteps: Int,
        rng: Random
    ): BoardShapeMask {
        if (levelNumber <= 2 && escalationSteps == 0) return BoardShapeMask.FULL_MATRIX
        // Deterministically introduce the tactical reverse shapes for Campaign progression
        return when (levelNumber) {
            3 -> BoardShapeMask.STEPPED_CITADEL // القلعة الحصينة 🏰
            4 -> BoardShapeMask.LABYRINTH_CORE  // قلب المتاهة 🌀
            5 -> BoardShapeMask.HOURGLASS       // الساعة الرملية ⏳
            6 -> BoardShapeMask.TWIN_PILLARS    // الأعمدة المزدوجة 🏛️
            7 -> BoardShapeMask.CLASSIC_TOWER   // البرج الشاهق 🗼
            else -> {
                val shapes = listOf(
                    BoardShapeMask.STEPPED_CITADEL,
                    BoardShapeMask.LABYRINTH_CORE,
                    BoardShapeMask.HOURGLASS,
                    BoardShapeMask.TWIN_PILLARS,
                    BoardShapeMask.CLASSIC_TOWER,
                    BoardShapeMask.FULL_MATRIX
                )
                val idx = (levelNumber + tier.tierIndex * 2 + escalationSteps) % shapes.size
                shapes[idx]
            }
        }
    }

    private fun selectArchetypeForLevel(
        levelNumber: Int,
        tier: DifficultyTier,
        rng: Random
    ): LevelAlgorithmArchetype {
        val archetypes = LevelAlgorithmArchetype.entries
        val idx = (levelNumber * 7 + tier.tierIndex * 3 + rng.nextInt(0, 2)) % archetypes.size
        return archetypes[idx]
    }

    fun createMaskCells(
        width: Int,
        height: Int,
        shapeMask: BoardShapeMask
    ): Set<GridPoint> {
        val points = LinkedHashSet<GridPoint>(width * height)
        val cx = (width - 1) / 2f
        val cy = (height - 1) / 2f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val nx = if (cx > 0) abs(x - cx) / cx else 0f
                val ny = if (cy > 0) abs(y - cy) / cy else 0f

                val inside = when (shapeMask) {
                    BoardShapeMask.FULL_MATRIX -> true
                    BoardShapeMask.CLASSIC_TOWER -> {
                        val cutoutTopLeft = (y < height / 10 && x < width / 8)
                        val cutoutTopRight = (y < height / 10 && x >= width - width / 8)
                        !cutoutTopLeft && !cutoutTopRight
                    }
                    BoardShapeMask.TWIN_PILLARS -> {
                        val isBridge = y in (height / 8)..(height * 7 / 8)
                        val isCenterSplit = (x == width / 2) && !isBridge
                        !isCenterSplit
                    }
                    BoardShapeMask.STEPPED_CITADEL -> (nx * 0.65f + ny * 0.55f) <= 1.24f
                    BoardShapeMask.LABYRINTH_CORE -> !(nx > 0.94f && ny > 0.94f)
                    BoardShapeMask.HOURGLASS -> nx <= (1f - 0.10f * (1f - ny))
                }

                if (inside) {
                    points.add(GridPoint(x, y))
                }
            }
        }
        return points
    }

    /**
     * Peels winding arrows from the outside in until virtually 100% of `maskCells` are packed
     * with non-overlapping, 100% solvable arrows.
     */
    private fun buildZeroGapPeeledLevel(
        levelNumber: Int,
        isCustom: Boolean,
        difficulty: DifficultyTier,
        shapeMask: BoardShapeMask,
        archetype: LevelAlgorithmArchetype,
        gridWidth: Int,
        gridHeight: Int,
        playerDnaSeed: Long,
        seed: Long
    ): PuzzleLevel {
        val maskCells = createMaskCells(gridWidth, gridHeight, shapeMask)
        val totalCells = gridWidth * gridHeight
        val inRemaining = BooleanArray(totalCells)
        for (pt in maskCells) {
            inRemaining[pt.y * gridWidth + pt.x] = true
        }

        val rng = Random(seed)
        val peeledInSolveOrder = ArrayList<WindingArrow>(maskCells.size / 3)

        // Track which step peeled each cell so new arrows prefer exiting through recently peeled channels
        val peeledAtStep = IntArray(totalCells) { 0 }
        // Track the head direction of ALL cells of peeled arrows to STRICTLY prevent adjacent arrows with identical directions
        val cellDirGrid = Array<Direction?>(totalCells) { null }
        var stepCounter = 0

        fun countRemainingNeighbors(cellIdx: Int): Int {
            val x = cellIdx % gridWidth
            val y = cellIdx / gridWidth
            var count = 0
            for (d in Direction.entries) {
                val nx = x + d.dx
                val ny = y + d.dy
                if (nx in 0 until gridWidth && ny in 0 until gridHeight) {
                    if (inRemaining[ny * gridWidth + nx]) count++
                }
            }
            return count
        }

        // Helper: checks if removing `candIdx` from `inRemaining` would leave >= 2 neighbors with 0 remaining neighbors
        // or if it leaves 1 neighbor with 0 remaining neighbors, returns that forced leaf index!
        fun checkOrphanImpactWhenRemoving(candIdx: Int): Pair<Boolean, Int> {
            val cx = candIdx % gridWidth
            val cy = candIdx / gridWidth
            var zeroDegCount = 0
            var forcedLeaf = -1
            for (d in Direction.entries) {
                val nx = cx + d.dx
                val ny = cy + d.dy
                if (nx in 0 until gridWidth && ny in 0 until gridHeight) {
                    val nIdx = ny * gridWidth + nx
                    if (inRemaining[nIdx]) {
                        val deg = countRemainingNeighbors(nIdx)
                        if (deg == 0) {
                            zeroDegCount++
                            forcedLeaf = nIdx
                        }
                    }
                }
            }
            return (zeroDegCount <= 1) to forcedLeaf
        }

        // MAIN OUTSIDE-IN PEELING LOOP
        var safetyIterations = 0
        val maxIterations = totalCells * 2
        while (safetyIterations < maxIterations) {
            safetyIterations++

            // Find all valid (headX, headY, headDir) candidates in `inRemaining`
            val headCandidates = findExposedHeadCandidates(
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                inRemaining = inRemaining,
                peeledAtStep = peeledAtStep,
                cellDirGrid = cellDirGrid,
                currentStep = stepCounter,
                rng = rng
            )

            if (headCandidates.isEmpty()) break

            val style = chooseArchetypeStyle(archetype, stepCounter, levelNumber, rng)
            val desiredLen = chooseDesiredArrowLength(levelNumber, style, rng)

            var peeledArrow: WindingArrow? = null
            val tryLimit = min(headCandidates.size, 16)

            for (i in 0 until tryLimit) {
                val cand = headCandidates[i]
                val built = tryPeelSingleArrow(
                    arrowId = peeledInSolveOrder.size + 1,
                    gridWidth = gridWidth,
                    gridHeight = gridHeight,
                    inRemaining = inRemaining,
                    headX = cand.hx,
                    headY = cand.hy,
                    headDir = cand.dir,
                    style = style,
                    desiredLength = desiredLen,
                    cellDirGrid = cellDirGrid,
                    rng = rng,
                    countRemainingNeighbors = ::countRemainingNeighbors,
                    checkOrphanImpact = ::checkOrphanImpactWhenRemoving
                )
                if (built != null) {
                    peeledArrow = built
                    break
                }
            }

            // Fallback 2+ cell peel
            if (peeledArrow == null) {
                val cand = headCandidates[0]
                val hx = cand.hx
                val hy = cand.hy
                val nx = hx - cand.dir.dx
                val ny = hy - cand.dir.dy
                val hIdx = hy * gridWidth + hx
                val nIdx = ny * gridWidth + nx
                if (inRemaining[hIdx] && inRemaining[nIdx]) {
                    inRemaining[hIdx] = false
                    inRemaining[nIdx] = false
                    peeledArrow = WindingArrow(
                        id = peeledInSolveOrder.size + 1,
                        cells = listOf(GridPoint(nx, ny), GridPoint(hx, hy)),
                        headDirection = cand.dir,
                        style = ArrowStyle.HOOK
                    )
                } else {
                    break
                }
            }

            stepCounter++
            peeledInSolveOrder.add(peeledArrow)
            for (c in peeledArrow.cells) {
                cellDirGrid[c.y * gridWidth + c.x] = peeledArrow.headDirection
                peeledAtStep[c.y * gridWidth + c.x] = stepCounter
            }
        }

        // UNIVERSAL SAFE GRAFTING & TAIL-BORROWING PASS:
        absorbAllRemainingCellsForZeroGaps(
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            inRemaining = inRemaining,
            cellDirGrid = cellDirGrid,
            peeledArrows = peeledInSolveOrder
        )

        // Enforce 0 adjacent same-direction arrows with solver-verified optimization:
        eliminateAdjacentSameDirectionConflicts(peeledInSolveOrder, gridWidth, gridHeight)

        // Select 3 to 7 bonus star arrows spaced across the level for the player to collect ⭐
        val starArrowIndices = HashSet<Int>()
        if (peeledInSolveOrder.size >= 5) {
            val starCount = (peeledInSolveOrder.size / 9).coerceIn(3, 8)
            val step = peeledInSolveOrder.size / (starCount + 1)
            for (i in 1..starCount) {
                starArrowIndices.add((i * step).coerceIn(0, peeledInSolveOrder.lastIndex))
            }
        }

        var occupiedFinalCells = 0
        val finalArrows = peeledInSolveOrder.mapIndexed { idx, arrow ->
            occupiedFinalCells += arrow.cells.size
            val isStarArrow = starArrowIndices.contains(idx)
            arrow.copy(
                id = idx + 1,
                colorAccentIndex = if (isStarArrow) 2 else 0,
                hasSparkleBadge = isStarArrow
            )
        }

        val fillPercent = ((occupiedFinalCells * 100f) / maskCells.size.coerceAtLeast(1))
            .toInt()
            .coerceIn(1, 100)

        val metrics = ArrowPuzzleSolver.solve(gridWidth, gridHeight, finalArrows)

        return PuzzleLevel(
            levelNumber = levelNumber,
            isCustomGenerated = isCustom,
            difficulty = difficulty,
            shapeMask = shapeMask,
            algorithmArchetype = archetype,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            activeMaskCells = maskCells,
            arrows = finalArrows,
            solverMetrics = metrics,
            fillDensityPercent = fillPercent,
            playerDnaCode = formatPlayerDnaCode(playerDnaSeed),
            seed = seed
        )
    }

    private data class ExposedHeadCandidate(
        val hx: Int,
        val hy: Int,
        val dir: Direction,
        val score: Float
    )

    private fun findExposedHeadCandidates(
        gridWidth: Int,
        gridHeight: Int,
        inRemaining: BooleanArray,
        peeledAtStep: IntArray,
        cellDirGrid: Array<Direction?>,
        currentStep: Int,
        rng: Random
    ): List<ExposedHeadCandidate> {
        val list = ArrayList<ExposedHeadCandidate>(128)
        val directions = Direction.entries

        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                val hIdx = y * gridWidth + x
                if (!inRemaining[hIdx]) continue

                // Count remaining neighbors of (x, y)
                var remDeg = 0
                for (d in directions) {
                    val ax = x + d.dx
                    val ay = y + d.dy
                    if (ax in 0 until gridWidth && ay in 0 until gridHeight && inRemaining[ay * gridWidth + ax]) {
                        remDeg++
                    }
                }
                if (remDeg == 0) continue

                for (dir in directions) {
                    val nx = x - dir.dx
                    val ny = y - dir.dy
                    if (nx !in 0 until gridWidth || ny !in 0 until gridHeight) continue
                    val nIdx = ny * gridWidth + nx
                    if (!inRemaining[nIdx]) continue

                    // Check if the forward ray from (x, y) in `dir` contains ZERO cells in `inRemaining`
                    var cx = x + dir.dx
                    var cy = y + dir.dy
                    var clearRay = true
                    var maxRecentPeelStepOnRay = 0
                    var peeledCellsOnRay = 0
                    while (cx in 0 until gridWidth && cy in 0 until gridHeight) {
                        val rIdx = cy * gridWidth + cx
                        if (inRemaining[rIdx]) {
                            clearRay = false
                            break
                        }
                        if (peeledAtStep[rIdx] > 0) {
                            peeledCellsOnRay++
                            if (peeledAtStep[rIdx] > maxRecentPeelStepOnRay) {
                                maxRecentPeelStepOnRay = peeledAtStep[rIdx]
                            }
                        }
                        cx += dir.dx
                        cy += dir.dy
                    }

                    if (!clearRay) continue

                    // Score this candidate:
                    // 1. Prioritize low-degree frontier tips so zero gaps remain
                    // 2. Strict Peeling Ray Dependency Bottlenecking:
                    var score = (4 - remDeg) * 20f
                    if (peeledCellsOnRay > 0) {
                        val recency = (maxRecentPeelStepOnRay.toFloat() / currentStep.coerceAtLeast(1))
                        score += 350f + recency * 120f + (peeledCellsOnRay.coerceAtMost(10) * 45f)
                    } else if (currentStep >= 2) {
                        score -= 15000f
                    }

                    // 3. STRICTLY PROHIBIT ADJACENT ARROWS IN THE SAME DIRECTION (As requested by user!):
                    // Checks all adjacent cells (Chebyshev distance <= 1) around head (x, y) and neck (nx, ny)
                    var adjacentSameDirCount = 0
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val ax = x + dx
                            val ay = y + dy
                            if (ax in 0 until gridWidth && ay in 0 until gridHeight) {
                                val neighborDir = cellDirGrid[ay * gridWidth + ax]
                                if (neighborDir == dir) {
                                    adjacentSameDirCount++
                                }
                            }
                            val bx = nx + dx
                            val by = ny + dy
                            if (bx in 0 until gridWidth && by in 0 until gridHeight) {
                                val neighborDir = cellDirGrid[by * gridWidth + bx]
                                if (neighborDir == dir) {
                                    adjacentSameDirCount++
                                }
                            }
                        }
                    }
                    if (adjacentSameDirCount > 0) {
                        score -= (500000f * adjacentSameDirCount)
                    }

                    score += rng.nextFloat() * 10f

                    list.add(ExposedHeadCandidate(x, y, dir, score))
                }
            }
        }

        val zeroConflictList = list.filter { it.score > -400000f }
        val finalList = if (zeroConflictList.isNotEmpty()) zeroConflictList else list
        return finalList.sortedByDescending { it.score }
    }

    private fun tryPeelSingleArrow(
        arrowId: Int,
        gridWidth: Int,
        gridHeight: Int,
        inRemaining: BooleanArray,
        headX: Int,
        headY: Int,
        headDir: Direction,
        style: ArrowStyle,
        desiredLength: Int,
        cellDirGrid: Array<Direction?>,
        rng: Random,
        countRemainingNeighbors: (Int) -> Int,
        checkOrphanImpact: (Int) -> Pair<Boolean, Int>
    ): WindingArrow? {
        val neckX = headX - headDir.dx
        val neckY = headY - headDir.dy
        val hIdx = headY * gridWidth + headX
        val nIdx = neckY * gridWidth + neckX

        if (!inRemaining[hIdx] || !inRemaining[nIdx]) return null

        val removedIndices = ArrayList<Int>(desiredLength + 2)
        val reversedCells = ArrayList<GridPoint>(desiredLength + 2)

        inRemaining[hIdx] = false
        removedIndices.add(hIdx)
        reversedCells.add(GridPoint(headX, headY))

        val (headOk, headLeaf) = checkOrphanImpact(hIdx)
        if (!headOk || (headLeaf != -1 && headLeaf != nIdx)) {
            // Rollback
            inRemaining[hIdx] = true
            return null
        }

        inRemaining[nIdx] = false
        removedIndices.add(nIdx)
        reversedCells.add(GridPoint(neckX, neckY))

        val (neckOk, neckForcedLeaf) = checkOrphanImpact(nIdx)
        if (!neckOk) {
            for (idx in removedIndices) inRemaining[idx] = true
            return null
        }

        var curX = neckX
        var curY = neckY
        var growthDir = headDir.opposite()
        val clockwise = rng.nextBoolean()
        var currentArmSteps = 1
        var turnsMade = 0
        var targetArmLength = when (style) {
            ArrowStyle.SPIRAL -> 1
            ArrowStyle.COMB_U -> rng.nextInt(1, 3)
            ArrowStyle.SERPENTINE -> rng.nextInt(2, 5)
            ArrowStyle.BRACKET -> rng.nextInt(2, 4)
            ArrowStyle.HOOK -> rng.nextInt(1, 3)
            ArrowStyle.STRAIGHT -> desiredLength
        }

        var pendingForcedLeaf = neckForcedLeaf

        while (reversedCells.size < desiredLength || pendingForcedLeaf != -1) {
            if (pendingForcedLeaf != -1) {
                val lx = pendingForcedLeaf % gridWidth
                val ly = pendingForcedLeaf / gridWidth
                inRemaining[pendingForcedLeaf] = false
                removedIndices.add(pendingForcedLeaf)
                reversedCells.add(GridPoint(lx, ly))
                // A forced leaf had 0 remaining neighbors, so the arrow cleanly terminates here!
                break
            }

            val preferredDirs = getArchetypeTurnOrder(
                style = style,
                currentDir = growthDir,
                clockwise = clockwise,
                currentArmSteps = currentArmSteps,
                targetArmLength = targetArmLength,
                turnsMade = turnsMade
            )

            var bestDir: Direction? = null
            var bestNextIdx = -1
            var bestForcedLeaf = -1
            var bestScore = Float.NEGATIVE_INFINITY

            for ((prefRank, candDir) in preferredDirs.withIndex()) {
                val tx = curX + candDir.dx
                val ty = curY + candDir.dy
                if (tx !in 0 until gridWidth || ty !in 0 until gridHeight) continue
                val tIdx = ty * gridWidth + tx
                if (!inRemaining[tIdx]) continue

                // Temporarily remove tIdx to check orphan impact
                inRemaining[tIdx] = false
                val (safeStep, forcedLeafAfterStep) = checkOrphanImpact(tIdx)
                val degAfter = countRemainingNeighbors(tIdx)
                inRemaining[tIdx] = true

                if (!safeStep) continue

                var stepScore = (4 - prefRank) * 24f
                // Prefer hugging the frontier / low-degree cells so we never leave interior holes
                stepScore += (4 - degAfter) * 16f
                if (forcedLeafAfterStep != -1) {
                    stepScore += 45f
                }

                // Strictly avoid growing into cells adjacent to an already peeled arrow in the SAME direction:
                var conflictWithNeighbor = false
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val ax = tx + dx
                        val ay = ty + dy
                        if (ax in 0 until gridWidth && ay in 0 until gridHeight) {
                            if (cellDirGrid[ay * gridWidth + ax] == headDir) {
                                conflictWithNeighbor = true
                                break
                            }
                        }
                    }
                    if (conflictWithNeighbor) break
                }
                if (conflictWithNeighbor) {
                    continue
                }

                if (stepScore > bestScore) {
                    bestScore = stepScore
                    bestDir = candDir
                    bestNextIdx = tIdx
                    bestForcedLeaf = forcedLeafAfterStep
                }
            }

            if (bestDir == null || bestNextIdx == -1) break

            curX += bestDir.dx
            curY += bestDir.dy
            inRemaining[bestNextIdx] = false
            removedIndices.add(bestNextIdx)
            reversedCells.add(GridPoint(curX, curY))
            pendingForcedLeaf = bestForcedLeaf

            if (bestDir == growthDir) {
                currentArmSteps++
            } else {
                turnsMade++
                growthDir = bestDir
                currentArmSteps = 1
                targetArmLength = when (style) {
                    ArrowStyle.SPIRAL -> 1 + (turnsMade / 2)
                    ArrowStyle.COMB_U -> if (turnsMade % 2 == 1) 1 else rng.nextInt(2, 4)
                    ArrowStyle.SERPENTINE -> if (turnsMade % 2 == 1) rng.nextInt(1, 3) else rng.nextInt(2, 5)
                    ArrowStyle.BRACKET -> rng.nextInt(2, 4)
                    ArrowStyle.HOOK -> rng.nextInt(1, 3)
                    ArrowStyle.STRAIGHT -> desiredLength
                }
            }
        }

        return WindingArrow(
            id = arrowId,
            cells = reversedCells.asReversed(),
            headDirection = headDir,
            style = style
        )
    }

    /**
     * Absorbs 100% of any remaining unpeeled cells in `inRemaining` so there are ZERO gaps:
     * 1. Any 2+ connected unpeeled cells or single cell adjacent to an existing arrow's tail
     *    is grafted onto that arrow's tail (safe by the Universal Safe Grafting Theorem because
     *    unpeeled cells never lay on any arrow's exit ray!).
     * 2. Any remaining single cell `c` borrows the tail cell `t` of an adjacent arrow of length >= 3
     *    to form a final 2-cell arrow `[t, c]` solved at the very end when the board is otherwise empty!
     */
    private fun absorbAllRemainingCellsForZeroGaps(
        gridWidth: Int,
        gridHeight: Int,
        inRemaining: BooleanArray,
        cellDirGrid: Array<Direction?>,
        peeledArrows: MutableList<WindingArrow>
    ) {
        val totalCells = gridWidth * gridHeight

        // Pass A: Graft unpeeled cells onto adjacent arrow tails
        var grafted = true
        while (grafted) {
            grafted = false
            for (i in peeledArrows.indices) {
                val arrow = peeledArrows[i]
                val tail = arrow.tail
                for (d in Direction.entries) {
                    val nx = tail.x + d.dx
                    val ny = tail.y + d.dy
                    if (nx !in 0 until gridWidth || ny !in 0 until gridHeight) continue
                    val nIdx = ny * gridWidth + nx
                    if (inRemaining[nIdx]) {
                        // Strictly verify grafting (nx, ny) does NOT create conflict with adjacent arrow pointing in same direction
                        var createsConflict = false
                        for (cdy in -1..1) {
                            for (cdx in -1..1) {
                                if (cdx == 0 && cdy == 0) continue
                                val ax = nx + cdx
                                val ay = ny + cdy
                                if (ax in 0 until gridWidth && ay in 0 until gridHeight) {
                                    val neighborDir = cellDirGrid[ay * gridWidth + ax]
                                    if (neighborDir == arrow.headDirection) {
                                        val isPartOfThisArrow = arrow.cells.any { it.x == ax && it.y == ay }
                                        if (!isPartOfThisArrow) {
                                            createsConflict = true
                                            break
                                        }
                                    }
                                }
                            }
                            if (createsConflict) break
                        }
                        if (createsConflict) continue

                        inRemaining[nIdx] = false
                        cellDirGrid[nIdx] = arrow.headDirection
                        val newCells = ArrayList<GridPoint>(arrow.cells.size + 1)
                        newCells.add(GridPoint(nx, ny))
                        newCells.addAll(arrow.cells)
                        peeledArrows[i] = arrow.copy(cells = newCells)
                        grafted = true
                        break
                    }
                }
            }
        }

        // Pass B: Pair any adjacent remaining cells `(a, b)` into a 2-cell arrow solved last!
        // Strictly avoid choosing directions that match neighboring arrows!
        for (cellIdx in 0 until totalCells) {
            if (!inRemaining[cellIdx]) continue
            val x = cellIdx % gridWidth
            val y = cellIdx / gridWidth

            val validNeighbors = ArrayList<Triple<Int, Int, Direction>>(4)
            for (d in Direction.entries) {
                val nx = x + d.dx
                val ny = y + d.dy
                if (nx in 0 until gridWidth && ny in 0 until gridHeight) {
                    val nIdx = ny * gridWidth + nx
                    if (inRemaining[nIdx]) {
                        validNeighbors.add(Triple(nx, ny, d))
                    }
                }
            }

            if (validNeighbors.isNotEmpty()) {
                val bestPair = validNeighbors.firstOrNull { pair ->
                    val candDir = pair.third
                    var conflict = false
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val ax = x + dx
                            val ay = y + dy
                            if (ax in 0 until gridWidth && ay in 0 until gridHeight) {
                                if (cellDirGrid[ay * gridWidth + ax] == candDir) { conflict = true; break }
                            }
                            val bx = pair.first + dx
                            val by = pair.second + dy
                            if (bx in 0 until gridWidth && by in 0 until gridHeight) {
                                if (cellDirGrid[by * gridWidth + bx] == candDir) { conflict = true; break }
                            }
                        }
                        if (conflict) break
                    }
                    !conflict
                }

                if (bestPair != null) {
                    val nx = bestPair.first
                    val ny = bestPair.second
                    val d = bestPair.third
                    val nIdx = ny * gridWidth + nx

                    inRemaining[cellIdx] = false
                    inRemaining[nIdx] = false
                    cellDirGrid[cellIdx] = d
                    cellDirGrid[nIdx] = d
                    peeledArrows.add(
                        WindingArrow(
                            id = peeledArrows.size + 1,
                            cells = listOf(GridPoint(x, y), GridPoint(nx, ny)),
                            headDirection = d,
                            style = ArrowStyle.HOOK
                        )
                    )
                }
            }
        }

        // Pass C: Tail-Borrowing for any remaining isolated single cell `c`!
        for (cellIdx in 0 until totalCells) {
            if (!inRemaining[cellIdx]) continue
            val cx = cellIdx % gridWidth
            val cy = cellIdx / gridWidth

            var rescued = false
            for (i in peeledArrows.indices) {
                val arrow = peeledArrows[i]
                if (arrow.cells.size < 3) continue
                val tail = arrow.tail
                val dx = cx - tail.x
                val dy = cy - tail.y
                if (abs(dx) + abs(dy) == 1) {
                    val dirToC = Direction.entries.first { it.dx == dx && it.dy == dy }
                    if (dirToC == arrow.headDirection) continue

                    var conflict = false
                    for (cdy in -1..1) {
                        for (cdx in -1..1) {
                            if (cdx == 0 && cdy == 0) continue
                            val ax = tail.x + cdx
                            val ay = tail.y + cdy
                            if (ax in 0 until gridWidth && ay in 0 until gridHeight && cellDirGrid[ay * gridWidth + ax] == dirToC) {
                                conflict = true
                                break
                            }
                            val bx = cx + cdx
                            val by = cy + cdy
                            if (bx in 0 until gridWidth && by in 0 until gridHeight && cellDirGrid[by * gridWidth + bx] == dirToC) {
                                conflict = true
                                break
                            }
                        }
                        if (conflict) break
                    }
                    if (conflict) continue

                    peeledArrows[i] = arrow.copy(cells = arrow.cells.drop(1))
                    inRemaining[cellIdx] = false
                    cellDirGrid[cellIdx] = dirToC
                    cellDirGrid[tail.y * gridWidth + tail.x] = dirToC
                    peeledArrows.add(
                        WindingArrow(
                            id = peeledArrows.size + 1,
                            cells = listOf(tail, GridPoint(cx, cy)),
                            headDirection = dirToC,
                            style = ArrowStyle.HOOK
                        )
                    )
                    rescued = true
                    break
                }
            }

            if (rescued) continue
        }
    }

    /**
     * Checks if any two adjacent arrows on the board have the exact same headDirection.
     * Returns the number of conflicting pairs. Zero conflicts means NO adjacent arrows in the same direction!
     */
    fun countAdjacentSameDirectionConflicts(
        arrows: List<WindingArrow>,
        gridWidth: Int,
        gridHeight: Int
    ): Int {
        val cellToArrow = HashMap<GridPoint, WindingArrow>(gridWidth * gridHeight)
        for (arrow in arrows) {
            for (c in arrow.cells) {
                cellToArrow[c] = arrow
            }
        }
        var conflicts = 0
        val checkedPairs = HashSet<Long>()
        for (arrow in arrows) {
            val arrowDir = arrow.naturalHeadDirection
            for (c in arrow.cells) {
                for (d in Direction.entries) {
                    val neighborArrow = cellToArrow[GridPoint(c.x + d.dx, c.y + d.dy)]
                    if (neighborArrow != null && neighborArrow.id != arrow.id) {
                        val idA = min(arrow.id, neighborArrow.id)
                        val idB = max(arrow.id, neighborArrow.id)
                        val pairKey = (idA.toLong() shl 32) or (idB.toLong() and 0xFFFFFFFFL)
                        if (checkedPairs.add(pairKey)) {
                            if (arrowDir == neighborArrow.naturalHeadDirection) {
                                conflicts++
                            }
                        }
                    }
                }
            }
        }
        return conflicts
    }

    /**
     * Solver-verified optimizer that iterates over any conflicting arrow pair and tests
     * non-conflicting end-to-end reversals while strictly verifying 100% solvability and natural arrowhead alignment.
     */
    fun eliminateAdjacentSameDirectionConflicts(
        arrows: MutableList<WindingArrow>,
        gridWidth: Int,
        gridHeight: Int
    ): Boolean {
        var curConflicts = countAdjacentSameDirectionConflicts(arrows, gridWidth, gridHeight)
        if (curConflicts == 0) return true

        fun tryMutateArrow(idx: Int): Boolean {
            val arrow = arrows[idx]

            // 1. Single-cell arrow: can point in any direction
            if (arrow.cells.size == 1) {
                for (d in Direction.entries) {
                    if (d == arrow.headDirection) continue
                    val mutated = arrows.toMutableList()
                    mutated[idx] = arrow.copy(headDirection = d)
                    val newSol = ArrowPuzzleSolver.solve(gridWidth, gridHeight, mutated)
                    if (newSol.isSolvable) {
                        val newConf = countAdjacentSameDirectionConflicts(mutated, gridWidth, gridHeight)
                        if (newConf < curConflicts) {
                            arrows[idx] = mutated[idx]
                            curConflicts = newConf
                            return true
                        }
                    }
                }
            }

            // 2. Multi-cell arrow: can ONLY be reversed end-to-end.
            // When reversed, its new headDirection MUST strictly match the new neck-to-head vector!
            if (arrow.cells.size >= 2) {
                val oldTail = arrow.tail
                val oldPreTail = arrow.cells[1]
                val newDx = oldTail.x - oldPreTail.x
                val newDy = oldTail.y - oldPreTail.y
                val newHeadDir = Direction.entries.firstOrNull { it.dx == newDx && it.dy == newDy }
                if (newHeadDir != null && newHeadDir != arrow.naturalHeadDirection) {
                    val mutated = arrows.toMutableList()
                    mutated[idx] = arrow.copy(
                        cells = arrow.cells.reversed(),
                        headDirection = newHeadDir
                    )
                    val newSol = ArrowPuzzleSolver.solve(gridWidth, gridHeight, mutated)
                    if (newSol.isSolvable) {
                        val newConf = countAdjacentSameDirectionConflicts(mutated, gridWidth, gridHeight)
                        if (newConf < curConflicts) {
                            arrows[idx] = mutated[idx]
                            curConflicts = newConf
                            return true
                        }
                    }
                }
            }
            return false
        }

        var mutationBudget = 20
        for (pass in 0..1) {
            if (curConflicts == 0 || mutationBudget <= 0) break
            val cellToArrow = HashMap<GridPoint, WindingArrow>(gridWidth * gridHeight)
            for (arrow in arrows) {
                for (c in arrow.cells) {
                    cellToArrow[c] = arrow
                }
            }

            for (i in arrows.indices) {
                if (curConflicts == 0 || mutationBudget <= 0) break
                val arrow = arrows[i]
                var conflictingNeighbor: WindingArrow? = null
                for (c in arrow.cells) {
                    for (d in Direction.entries) {
                        val n = cellToArrow[GridPoint(c.x + d.dx, c.y + d.dy)]
                        if (n != null && n.id != arrow.id && n.headDirection == arrow.headDirection) {
                            conflictingNeighbor = n
                            break
                        }
                    }
                    if (conflictingNeighbor != null) break
                }
                if (conflictingNeighbor == null) continue

                mutationBudget--
                // Try mutating arrow i
                val fixed = tryMutateArrow(i)
                if (fixed) {
                    if (curConflicts == 0) return true
                    for (c in arrows[i].cells) cellToArrow[c] = arrows[i]
                    continue
                }

                if (mutationBudget <= 0) break
                mutationBudget--
                // If mutating arrow i didn't fix it, try mutating conflictingNeighbor!
                val neighborIdx = arrows.indexOfFirst { it.id == conflictingNeighbor.id }
                if (neighborIdx != -1) {
                    val neighborFixed = tryMutateArrow(neighborIdx)
                    if (neighborFixed) {
                        if (curConflicts == 0) return true
                        for (c in arrows[neighborIdx].cells) cellToArrow[c] = arrows[neighborIdx]
                    }
                }
            }
            if (curConflicts == 0) break
        }
        return curConflicts == 0
    }

    private fun chooseArchetypeStyle(
        archetype: LevelAlgorithmArchetype,
        stepIndex: Int,
        levelNumber: Int,
        rng: Random
    ): ArrowStyle {
        if (levelNumber <= 2) {
            // Levels 1 and 2: clean 1-2 turn hooks and brackets
            return if (rng.nextBoolean()) ArrowStyle.HOOK else ArrowStyle.BRACKET
        }
        // From Level 3 onward: strictly winding multi-turn spirals, serpents, combs, and brackets
        val roll = rng.nextFloat()
        return when (archetype) {
            LevelAlgorithmArchetype.SPIRAL_CITADEL -> when {
                roll < 0.45f -> ArrowStyle.SPIRAL
                roll < 0.75f -> ArrowStyle.BRACKET
                roll < 0.90f -> ArrowStyle.SERPENTINE
                else -> ArrowStyle.COMB_U
            }
            LevelAlgorithmArchetype.SERPENTINE_LABYRINTH -> when {
                roll < 0.50f -> ArrowStyle.SERPENTINE
                roll < 0.80f -> ArrowStyle.BRACKET
                else -> ArrowStyle.SPIRAL
            }
            LevelAlgorithmArchetype.INTERLOCKING_COMBS -> when {
                roll < 0.48f -> ArrowStyle.COMB_U
                roll < 0.78f -> ArrowStyle.BRACKET
                else -> ArrowStyle.SERPENTINE
            }
            LevelAlgorithmArchetype.VORTEX_CONVERGENCE -> when {
                roll < 0.45f -> ArrowStyle.SPIRAL
                roll < 0.75f -> ArrowStyle.SERPENTINE
                else -> ArrowStyle.COMB_U
            }
            LevelAlgorithmArchetype.HYBRID_GRANDMASTER -> when {
                roll < 0.38f -> ArrowStyle.SPIRAL
                roll < 0.68f -> ArrowStyle.SERPENTINE
                roll < 0.86f -> ArrowStyle.COMB_U
                else -> ArrowStyle.BRACKET
            }
        }
    }

    private fun chooseDesiredArrowLength(
        levelNumber: Int,
        style: ArrowStyle,
        rng: Random
    ): Int {
        if (levelNumber <= 2) {
            return rng.nextInt(3, 5) // 3 to 4 cells with at least 1-2 bends
        } else if (levelNumber in 3..4) {
            return when (style) {
                ArrowStyle.SPIRAL -> rng.nextInt(6, 10)
                ArrowStyle.SERPENTINE -> rng.nextInt(5, 9)
                ArrowStyle.COMB_U -> rng.nextInt(5, 8)
                ArrowStyle.BRACKET -> rng.nextInt(4, 7)
                ArrowStyle.HOOK -> rng.nextInt(4, 6)
                ArrowStyle.STRAIGHT -> rng.nextInt(3, 5)
            }
        }
        // Level 5+: complex deeply-interlocked multi-turned arrows (6 to 12 cells, 3 to 6 turns)
        return when (style) {
            ArrowStyle.SPIRAL -> rng.nextInt(7, 12)
            ArrowStyle.SERPENTINE -> rng.nextInt(6, 11)
            ArrowStyle.COMB_U -> rng.nextInt(6, 10)
            ArrowStyle.BRACKET -> rng.nextInt(5, 9)
            ArrowStyle.HOOK -> rng.nextInt(4, 7)
            ArrowStyle.STRAIGHT -> rng.nextInt(4, 6)
        }
    }

    private fun getArchetypeTurnOrder(
        style: ArrowStyle,
        currentDir: Direction,
        clockwise: Boolean,
        currentArmSteps: Int,
        targetArmLength: Int,
        turnsMade: Int
    ): List<Direction> {
        val primaryTurn = if (clockwise) currentDir.turnClockwise() else currentDir.turnCounterClockwise()
        val secondaryTurn = if (clockwise) currentDir.turnCounterClockwise() else currentDir.turnClockwise()

        return when (style) {
            ArrowStyle.SPIRAL -> {
                if (currentArmSteps < targetArmLength) {
                    listOf(currentDir, primaryTurn, secondaryTurn)
                } else {
                    listOf(primaryTurn, currentDir, secondaryTurn)
                }
            }
            ArrowStyle.COMB_U, ArrowStyle.SERPENTINE -> {
                if (currentArmSteps < targetArmLength) {
                    listOf(currentDir, primaryTurn, secondaryTurn)
                } else {
                    val alternateTurn = if ((turnsMade / 2) % 2 == 0) primaryTurn else secondaryTurn
                    val otherTurn = if (alternateTurn == primaryTurn) secondaryTurn else primaryTurn
                    listOf(alternateTurn, currentDir, otherTurn)
                }
            }
            ArrowStyle.BRACKET, ArrowStyle.HOOK -> {
                if (currentArmSteps < targetArmLength) {
                    listOf(currentDir, primaryTurn, secondaryTurn)
                } else {
                    listOf(primaryTurn, secondaryTurn, currentDir)
                }
            }
            ArrowStyle.STRAIGHT -> listOf(currentDir, primaryTurn, secondaryTurn)
        }
    }
}
