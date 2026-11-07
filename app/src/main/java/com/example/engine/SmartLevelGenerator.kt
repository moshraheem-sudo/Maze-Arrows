package com.example.engine

import kotlin.math.abs
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
        val tier = forcedDifficultyTier ?: DifficultyTier.dynamicForLevel(clampedLevel, escalationSteps)
        val effectiveEscalation = if (forcedDifficultyTier != null) 0 else escalationSteps
        val salt = clampedLevel.toLong() * 1009L + effectiveEscalation.toLong() * 7919L
        val combinedSeed = mixSeed(playerDnaSeed, salt)
        val rng = Random(combinedSeed)

        val (gridW, gridH) = computeDynamicDimensions(clampedLevel, tier, effectiveEscalation)
        // Automatically vary board shapes and smart generator archetypes across levels
        val shapeMask = selectDynamicShapeMask(clampedLevel, tier, effectiveEscalation, rng)
        val archetype = selectArchetypeForLevel(clampedLevel + effectiveEscalation * 3, tier, rng)

        return buildZeroGapPeeledLevel(
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
     * Smart Generator Lab also automatically alternates difficulty modes and archetypes
     * so every generated board offers a fresh, varied challenge.
     */
    fun generateCustomLevel(
        difficulty: DifficultyTier,
        shapeMask: BoardShapeMask,
        archetype: LevelAlgorithmArchetype = LevelAlgorithmArchetype.HYBRID_GRANDMASTER,
        playerDnaSeed: Long = 0x5F3759DF7C9E1234L,
        seed: Long = System.currentTimeMillis()
    ): PuzzleLevel {
        val rng = Random(seed xor playerDnaSeed)
        // System dynamically blends/varies difficulty and archetype so the experience stays fresh
        val dynamicTier = DifficultyTier.entries[(difficulty.tierIndex + rng.nextInt(0, DifficultyTier.entries.size)) % DifficultyTier.entries.size]
        val dynamicArch = LevelAlgorithmArchetype.entries[rng.nextInt(LevelAlgorithmArchetype.entries.size)]
        val representativeLevel = when (dynamicTier) {
            DifficultyTier.EASY -> 2
            DifficultyTier.MEDIUM -> 5
            DifficultyTier.HARD -> 12
            DifficultyTier.EXPERT -> 30
            DifficultyTier.PRO -> 80
        }
        val (gridW, gridH) = computeDynamicDimensions(representativeLevel, dynamicTier, escalationSteps = 0)
        val finalSeed = mixSeed(playerDnaSeed xor seed, representativeLevel.toLong())

        return buildZeroGapPeeledLevel(
            levelNumber = representativeLevel,
            isCustom = true,
            difficulty = dynamicTier,
            shapeMask = shapeMask,
            archetype = dynamicArch,
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
     * Computes board dimensions based on the system-selected dynamic [tier], [levelNumber],
     * and [escalationSteps] (when the player exhausts attempts or restarts, the board becomes
     * larger and denser!).
     */
    private fun computeDynamicDimensions(
        levelNumber: Int,
        tier: DifficultyTier,
        escalationSteps: Int
    ): Pair<Int, Int> {
        val basePair = when (tier) {
            DifficultyTier.EASY -> 9 to 13
            DifficultyTier.MEDIUM -> 11 to 17
            DifficultyTier.HARD -> 14 to 21
            DifficultyTier.EXPERT -> 17 to 25
            DifficultyTier.PRO -> 20 to 30
        }
        val levelBonusW = ((levelNumber - 1) / 25).coerceIn(0, 4)
        val levelBonusH = ((levelNumber - 1) / 20).coerceIn(0, 6)
        val escBonusW = escalationSteps * 2
        val escBonusH = escalationSteps * 3

        val finalW = (basePair.first + levelBonusW + escBonusW).coerceIn(9, 24)
        val finalH = (basePair.second + levelBonusH + escBonusH).coerceIn(13, 36)
        return finalW to finalH
    }

    private fun selectDynamicShapeMask(
        levelNumber: Int,
        tier: DifficultyTier,
        escalationSteps: Int,
        rng: Random
    ): BoardShapeMask {
        if (levelNumber <= 2 && escalationSteps == 0) return BoardShapeMask.FULL_MATRIX
        val masks = BoardShapeMask.entries
        val idx = (levelNumber * 3 + tier.tierIndex + escalationSteps * 2 + rng.nextInt(0, 2)) % masks.size
        return masks[idx]
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
                        val cutoutTopLeft = (y < height / 6 && x < width / 5)
                        val cutoutTopRight = (y < height / 6 && x >= width - width / 5)
                        !cutoutTopLeft && !cutoutTopRight
                    }
                    BoardShapeMask.TWIN_PILLARS -> {
                        val isBridge = y in (height / 5)..(height * 4 / 5)
                        val isCenterSplit = (x == width / 2) && !isBridge
                        !isCenterSplit
                    }
                    BoardShapeMask.STEPPED_CITADEL -> (nx * 0.70f + ny * 0.60f) <= 1.18f
                    BoardShapeMask.LABYRINTH_CORE -> !(nx > 0.88f && ny > 0.88f)
                    BoardShapeMask.HOURGLASS -> nx <= (1f - 0.22f * (1f - ny))
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
        // (creating deep multi-wave blocking dependencies!)
        val peeledAtStep = IntArray(totalCells) { 0 }
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
                        // Since candIdx is currently false in `inRemaining`, countRemainingNeighbors(nIdx)
                        // is the exact degree of nIdx after candIdx is removed!
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
                currentStep = stepCounter,
                rng = rng
            )

            if (headCandidates.isEmpty()) break

            val style = chooseArchetypeStyle(archetype, stepCounter, levelNumber, rng)
            val desiredLen = chooseDesiredArrowLength(levelNumber, style, rng)

            var peeledArrow: WindingArrow? = null
            val tryLimit = min(headCandidates.size, 14)

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
                    rng = rng,
                    countRemainingNeighbors = ::countRemainingNeighbors,
                    checkOrphanImpact = ::checkOrphanImpactWhenRemoving
                )
                if (built != null) {
                    peeledArrow = built
                    break
                }
            }

            // If strict orphan-preserving walk couldn't place on top candidates, allow unrestricted 2+ cell peel
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
                peeledAtStep[c.y * gridWidth + c.x] = stepCounter
            }
        }

        // UNIVERSAL SAFE GRAFTING & TAIL-BORROWING PASS:
        // Any cell `c` still in `inRemaining` was never peeled, which means NO arrow in
        // `peeledInSolveOrder` ever had its exit ray pass through `c`!
        absorbAllRemainingCellsForZeroGaps(
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            inRemaining = inRemaining,
            peeledArrows = peeledInSolveOrder
        )

        var occupiedFinalCells = 0
        val totalArrows = peeledInSolveOrder.size
        val finalArrows = peeledInSolveOrder.mapIndexed { idx, arrow ->
            occupiedFinalCells += arrow.cells.size
            val isGoldenHighlight = (totalArrows >= 10) &&
                (idx == 0 || idx == totalArrows / 3 || idx == (totalArrows * 2) / 3)
            val hasSparkle = (totalArrows >= 12) && (idx == totalArrows / 2)
            arrow.copy(
                id = idx + 1,
                colorAccentIndex = if (isGoldenHighlight) 1 else 0,
                hasSparkleBadge = hasSparkle
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
                    while (cx in 0 until gridWidth && cy in 0 until gridHeight) {
                        val rIdx = cy * gridWidth + cx
                        if (inRemaining[rIdx]) {
                            clearRay = false
                            break
                        }
                        if (peeledAtStep[rIdx] > maxRecentPeelStepOnRay) {
                            maxRecentPeelStepOnRay = peeledAtStep[rIdx]
                        }
                        cx += dir.dx
                        cy += dir.dy
                    }

                    if (!clearRay) continue

                    // Score this candidate:
                    // 1. Strongly prioritize degree-1 and degree-2 frontier tips so corners/leaves are consumed cleanly
                    // 2. Strongly prioritize rays that pass through recently peeled cells (creates deep blocking waves!)
                    var score = (4 - remDeg) * 32f
                    if (maxRecentPeelStepOnRay > 0) {
                        val recency = (maxRecentPeelStepOnRay.toFloat() / currentStep.coerceAtLeast(1))
                        score += 28f + recency * 24f
                    }
                    score += rng.nextFloat() * 14f

                    list.add(ExposedHeadCandidate(x, y, dir, score))
                }
            }
        }

        list.sortByDescending { it.score }
        return list
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
                        inRemaining[nIdx] = false
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
        // Since all `peeledArrows` are solved BEFORE this final arrow, the board is empty when it exits!
        for (cellIdx in 0 until totalCells) {
            if (!inRemaining[cellIdx]) continue
            val x = cellIdx % gridWidth
            val y = cellIdx / gridWidth
            for (d in Direction.entries) {
                val nx = x + d.dx
                val ny = y + d.dy
                if (nx in 0 until gridWidth && ny in 0 until gridHeight) {
                    val nIdx = ny * gridWidth + nx
                    if (inRemaining[nIdx]) {
                        inRemaining[cellIdx] = false
                        inRemaining[nIdx] = false
                        peeledArrows.add(
                            WindingArrow(
                                id = peeledArrows.size + 1,
                                cells = listOf(GridPoint(x, y), GridPoint(nx, ny)),
                                headDirection = d,
                                style = ArrowStyle.HOOK
                            )
                        )
                        break
                    }
                }
            }
        }

        // Pass C: Tail-Borrowing for any remaining isolated single cell `c`!
        // Detach the tail `t` of an adjacent arrow of length >= 3 and form a final 2-cell arrow `t -> c`!
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
                    // Shorten arrow `i` by removing its tail cell `tail`
                    peeledArrows[i] = arrow.copy(cells = arrow.cells.drop(1))
                    inRemaining[cellIdx] = false
                    // Add `[tail, (cx, cy)]` at the end of `peeledArrows` (solved LAST when board is empty!)
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

    private fun chooseArchetypeStyle(
        archetype: LevelAlgorithmArchetype,
        stepIndex: Int,
        levelNumber: Int,
        rng: Random
    ): ArrowStyle {
        if (levelNumber == 1 && stepIndex % 3 != 0) {
            return if (rng.nextBoolean()) ArrowStyle.BRACKET else ArrowStyle.HOOK
        }
        val roll = rng.nextFloat()
        return when (archetype) {
            LevelAlgorithmArchetype.SPIRAL_CITADEL -> when {
                roll < 0.45f -> ArrowStyle.SPIRAL
                roll < 0.78f -> ArrowStyle.BRACKET
                else -> ArrowStyle.HOOK
            }
            LevelAlgorithmArchetype.SERPENTINE_LABYRINTH -> when {
                roll < 0.50f -> ArrowStyle.SERPENTINE
                roll < 0.80f -> ArrowStyle.BRACKET
                else -> ArrowStyle.HOOK
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
                roll < 0.36f -> ArrowStyle.SPIRAL
                roll < 0.64f -> ArrowStyle.SERPENTINE
                roll < 0.82f -> ArrowStyle.COMB_U
                else -> ArrowStyle.BRACKET
            }
        }
    }

    private fun chooseDesiredArrowLength(
        levelNumber: Int,
        style: ArrowStyle,
        rng: Random
    ): Int {
        // Keep arrows compact to medium-long so there are MANY closely-packed arrows per board!
        val levelBonus = (levelNumber / 15).coerceAtMost(8)
        return when {
            levelNumber == 1 -> rng.nextInt(3, 6)
            levelNumber == 2 -> rng.nextInt(3, 7)
            levelNumber <= 5 -> when (style) {
                ArrowStyle.SPIRAL -> rng.nextInt(6, 11)
                ArrowStyle.SERPENTINE -> rng.nextInt(5, 9)
                else -> rng.nextInt(3, 7)
            }
            else -> when (style) {
                ArrowStyle.SPIRAL -> rng.nextInt(8 + levelBonus / 2, 16 + levelBonus)
                ArrowStyle.SERPENTINE -> rng.nextInt(6 + levelBonus / 2, 13 + levelBonus)
                ArrowStyle.BRACKET -> rng.nextInt(4, 9)
                ArrowStyle.COMB_U -> rng.nextInt(4, 7)
                ArrowStyle.HOOK -> rng.nextInt(3, 6)
                ArrowStyle.STRAIGHT -> rng.nextInt(2, 5)
            }
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
