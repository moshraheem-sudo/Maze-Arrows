package com.example

import com.example.engine.ArrowPuzzleSolver
import com.example.engine.BoardShapeMask
import com.example.engine.DifficultyTier
import com.example.engine.GridPoint
import com.example.engine.LevelAlgorithmArchetype
import com.example.engine.SmartLevelGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {

    @Test
    fun progressiveLevelsFromEasiestToHardest_haveZeroGapsAndIncreasingArrows() {
        val level1 = SmartLevelGenerator.generateCampaignLevel(1, playerDnaSeed = 1001L)
        val level2 = SmartLevelGenerator.generateCampaignLevel(2, playerDnaSeed = 1001L)
        val level5 = SmartLevelGenerator.generateCampaignLevel(5, playerDnaSeed = 1001L)
        val level25 = SmartLevelGenerator.generateCampaignLevel(25, playerDnaSeed = 1001L)
        val level250 = SmartLevelGenerator.generateCampaignLevel(
            SmartLevelGenerator.MASTERPIECE_SHOWCASE_LEVEL,
            playerDnaSeed = 1001L
        )

        // Verify progressive growth on EVERY level: Level 1 < Level 2 < Level 5 < Level 25 < Level 250
        assertTrue(
            "Level 2 (${level2.arrows.size}) must have more arrows than Level 1 (${level1.arrows.size})",
            level2.arrows.size > level1.arrows.size
        )
        assertTrue(
            "Level 5 (${level5.arrows.size}) must have more arrows than Level 2 (${level2.arrows.size})",
            level5.arrows.size > level2.arrows.size
        )
        assertTrue(
            "Level 25 (${level25.arrows.size}) must have more arrows than Level 5 (${level5.arrows.size})",
            level25.arrows.size > level5.arrows.size
        )
        assertTrue(
            "Level 250 (${level250.arrows.size}) must be a massive master board with at least 150 arrows",
            level250.arrows.size >= 150
        )

        val sampleLevels = listOf(level1, level2, level5, level25, level250)
        for (level in sampleLevels) {
            assertTrue(
                "Level ${level.levelNumber} must have 98-100% zero-gap density (was ${level.fillDensityPercent}%)",
                level.fillDensityPercent >= 98
            )
            assertTrue(
                "Level ${level.levelNumber} must be 100% solvable",
                level.solverMetrics.isSolvable
            )
            assertEquals(level.arrows.size, level.solverMetrics.solutionOrder.size)

            val seenCells = HashSet<GridPoint>()
            for (arrow in level.arrows) {
                assertTrue("Arrow must have at least 2 cells", arrow.cells.size >= 2)
                for (i in 0 until arrow.cells.size - 1) {
                    val a = arrow.cells[i]
                    val b = arrow.cells[i + 1]
                    val manhattan = abs(a.x - b.x) + abs(a.y - b.y)
                    assertEquals("Adjacent cells in arrow must be 4-connected", 1, manhattan)
                }
                for (cell in arrow.cells) {
                    assertTrue(
                        "Cell $cell in level ${level.levelNumber} must not overlap another arrow",
                        seenCells.add(cell)
                    )
                }
            }

            val hint = ArrowPuzzleSolver.findBestHintArrow(
                level.gridWidth,
                level.gridHeight,
                level.arrows
            )
            assertNotNull("Level ${level.levelNumber} must have at least one valid hint arrow", hint)
        }
    }

    @Test
    fun perPlayerDnaAndArchetypes_produceUniqueZeroGapSolvableLevels() {
        val playerASeed = 0x1122334455667788L
        val playerBSeed = 0x55AABBCCDDEEFF00L

        val levelForPlayerA = SmartLevelGenerator.generateCampaignLevel(10, playerASeed)
        val levelForPlayerB = SmartLevelGenerator.generateCampaignLevel(10, playerBSeed)

        assertTrue(levelForPlayerA.solverMetrics.isSolvable)
        assertTrue(levelForPlayerB.solverMetrics.isSolvable)
        assertNotEquals(
            "Two different players must receive different arrow configurations for the same level",
            levelForPlayerA.arrows.map { it.cells },
            levelForPlayerB.arrows.map { it.cells }
        )

        for (archetype in LevelAlgorithmArchetype.entries) {
            val customLevel = SmartLevelGenerator.generateCustomLevel(
                difficulty = DifficultyTier.HARD,
                shapeMask = BoardShapeMask.FULL_MATRIX,
                archetype = archetype,
                playerDnaSeed = playerASeed,
                seed = 777L + archetype.ordinal
            )
            assertTrue("Archetype $archetype must be 100% solvable", customLevel.solverMetrics.isSolvable)
            assertTrue(
                "Archetype $archetype must have >= 98% zero-gap density (was ${customLevel.fillDensityPercent}%)",
                customLevel.fillDensityPercent >= 98
            )
        }
    }
}
