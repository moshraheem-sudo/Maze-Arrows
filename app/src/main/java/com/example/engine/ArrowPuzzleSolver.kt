package com.example.engine

/**
 * Complete Solver & Move Verifier for Amaze Arrows.
 * Uses [GridRaycaster] (Java) to perform fast ray-collision checks, iterative topological
 * peeling (wave elimination), strategic hint selection, and full solvability verification.
 */
object ArrowPuzzleSolver {

    /**
     * Builds a flat grid array of size (gridWidth * gridHeight) where each cell contains
     * the occupying arrow's ID, or -1 if empty.
     */
    fun buildOccupancyGrid(
        gridWidth: Int,
        gridHeight: Int,
        arrows: Collection<WindingArrow>
    ): IntArray {
        val grid = IntArray(gridWidth * gridHeight) { -1 }
        for (arrow in arrows) {
            for (cell in arrow.cells) {
                if (cell.x in 0 until gridWidth && cell.y in 0 until gridHeight) {
                    grid[cell.y * gridWidth + cell.x] = arrow.id
                }
            }
        }
        return grid
    }

    /**
     * Checks whether [arrow] can exit the board without colliding with any arrow in [activeArrows].
     */
    fun checkMove(
        gridWidth: Int,
        gridHeight: Int,
        activeArrows: Collection<WindingArrow>,
        arrow: WindingArrow,
        precomputedGrid: IntArray? = null
    ): MoveCheckResult {
        val grid = precomputedGrid ?: buildOccupancyGrid(gridWidth, gridHeight, activeArrows)
        val head = arrow.head
        val dir = arrow.headDirection

        val blockerId = GridRaycaster.findFirstBlocker(
            gridWidth,
            gridHeight,
            grid,
            head.x,
            head.y,
            dir.dx,
            dir.dy,
            arrow.id
        )

        val clearSteps = GridRaycaster.countClearSteps(
            gridWidth,
            gridHeight,
            grid,
            head.x,
            head.y,
            dir.dx,
            dir.dy,
            arrow.id
        )

        return if (blockerId == -1) {
            MoveCheckResult.Clear(
                arrowId = arrow.id,
                stepsToEdge = clearSteps + arrow.length + 2
            )
        } else {
            val collisionX = (head.x + dir.dx * (clearSteps + 1)).coerceIn(0, gridWidth - 1)
            val collisionY = (head.y + dir.dy * (clearSteps + 1)).coerceIn(0, gridHeight - 1)
            MoveCheckResult.Blocked(
                arrowId = arrow.id,
                blockerArrowId = blockerId,
                collisionPoint = GridPoint(collisionX, collisionY),
                clearStepsBeforeCollision = clearSteps
            )
        }
    }

    /**
     * Returns all arrows that can currently be moved out of the board without collision.
     */
    fun getMovableArrows(
        gridWidth: Int,
        gridHeight: Int,
        activeArrows: List<WindingArrow>
    ): List<WindingArrow> {
        if (activeArrows.isEmpty()) return emptyList()
        val grid = buildOccupancyGrid(gridWidth, gridHeight, activeArrows)
        return activeArrows.filter { arrow ->
            GridRaycaster.findFirstBlocker(
                gridWidth,
                gridHeight,
                grid,
                arrow.head.x,
                arrow.head.y,
                arrow.headDirection.dx,
                arrow.headDirection.dy,
                arrow.id
            ) == -1
        }
    }

    /**
     * Finds the most strategic movable arrow to suggest as a Hint:
     * Prioritizes a movable arrow whose body cells currently block the exit rays of the largest
     * number of blocked arrows, or the longest winding arrow among movable candidates.
     */
    fun findBestHintArrow(
        gridWidth: Int,
        gridHeight: Int,
        activeArrows: List<WindingArrow>
    ): WindingArrow? {
        if (activeArrows.isEmpty()) return null
        val grid = buildOccupancyGrid(gridWidth, gridHeight, activeArrows)
        val movable = mutableListOf<WindingArrow>()
        val blockedCountByBlocker = HashMap<Int, Int>()

        for (arrow in activeArrows) {
            val blockerId = GridRaycaster.findFirstBlocker(
                gridWidth,
                gridHeight,
                grid,
                arrow.head.x,
                arrow.head.y,
                arrow.headDirection.dx,
                arrow.headDirection.dy,
                arrow.id
            )
            if (blockerId == -1) {
                movable.add(arrow)
            } else {
                blockedCountByBlocker[blockerId] = (blockedCountByBlocker[blockerId] ?: 0) + 1
            }
        }

        if (movable.isEmpty()) return null
        return movable.maxWithOrNull(
            compareBy<WindingArrow> { blockedCountByBlocker[it.id] ?: 0 }
                .thenBy { it.length }
        )
    }

    /**
     * Runs the complete topological elimination solver on a board state.
     * Verifies if all arrows can be cleared and computes [SolverMetrics].
     */
    fun solve(
        gridWidth: Int,
        gridHeight: Int,
        arrows: List<WindingArrow>
    ): SolverMetrics {
        if (arrows.isEmpty()) {
            return SolverMetrics(true, 0, 0, 0, emptyList(), 0)
        }

        val remaining = LinkedHashMap<Int, WindingArrow>(arrows.size)
        for (a in arrows) {
            remaining[a.id] = a
        }

        val grid = buildOccupancyGrid(gridWidth, gridHeight, arrows)
        val solutionOrder = ArrayList<Int>(arrows.size)
        var initialMovable = 0
        var waves = 0
        var totalTurns = 0
        for (a in arrows) {
            if (a.cells.size >= 3) {
                for (i in 1 until a.cells.size - 1) {
                    val prev = a.cells[i - 1]
                    val next = a.cells[i + 1]
                    if (prev.x != next.x && prev.y != next.y) {
                        totalTurns++
                    }
                }
            }
        }

        while (remaining.isNotEmpty()) {
            val currentWave = ArrayList<WindingArrow>()
            for (arrow in remaining.values) {
                val blocker = GridRaycaster.findFirstBlocker(
                    gridWidth,
                    gridHeight,
                    grid,
                    arrow.head.x,
                    arrow.head.y,
                    arrow.headDirection.dx,
                    arrow.headDirection.dy,
                    arrow.id
                )
                if (blocker == -1) {
                    currentWave.add(arrow)
                }
            }

            if (currentWave.isEmpty()) {
                // Deadlock detected
                return SolverMetrics(
                    false,
                    arrows.size,
                    initialMovable,
                    waves,
                    solutionOrder,
                    0
                )
            }

            if (waves == 0) {
                initialMovable = currentWave.size
            }
            waves++

            // Sort wave so longer / more complex arrows are recorded deterministically
            currentWave.sortByDescending { it.length }

            for (clearedArrow in currentWave) {
                remaining.remove(clearedArrow.id)
                solutionOrder.add(clearedArrow.id)
                for (cell in clearedArrow.cells) {
                    if (cell.x in 0 until gridWidth && cell.y in 0 until gridHeight) {
                        grid[cell.y * gridWidth + cell.x] = -1
                    }
                }
            }
        }

        val complexityScore = (arrows.size * 12) + (waves * 25) + (totalTurns * 4)
        return SolverMetrics(
            true,
            arrows.size,
            initialMovable,
            waves,
            solutionOrder,
            complexityScore
        )
    }

    /**
     * Traces a straight line from [arrow]'s head all the way to the board edge along [arrow.headDirection],
     * collecting all grid cells along that straight line where other arrows intersect/overlap,
     * as well as the IDs of all intersecting arrows.
     */
    fun inspectStraightLineTrajectory(
        gridWidth: Int,
        gridHeight: Int,
        activeArrows: Collection<WindingArrow>,
        arrow: WindingArrow
    ): StraightLineInspection {
        val grid = buildOccupancyGrid(gridWidth, gridHeight, activeArrows)
        val head = arrow.head
        val dir = arrow.headDirection
        val intersectingPoints = ArrayList<GridPoint>()
        val intersectingArrowIds = LinkedHashSet<Int>()

        var cx = head.x + dir.dx
        var cy = head.y + dir.dy
        while (cx in 0 until gridWidth && cy in 0 until gridHeight) {
            val occupantId = grid[cy * gridWidth + cx]
            if (occupantId != -1 && occupantId != arrow.id) {
                intersectingPoints.add(GridPoint(cx, cy))
                intersectingArrowIds.add(occupantId)
            }
            cx += dir.dx
            cy += dir.dy
        }

        return StraightLineInspection(
            arrowId = arrow.id,
            head = head,
            direction = dir,
            intersectingPoints = intersectingPoints,
            intersectingArrowIds = intersectingArrowIds
        )
    }
}

data class StraightLineInspection(
    val arrowId: Int,
    val head: GridPoint,
    val direction: Direction,
    val intersectingPoints: List<GridPoint>,
    val intersectingArrowIds: Set<Int>
) {
    val isClear: Boolean get() = intersectingPoints.isEmpty()
}

