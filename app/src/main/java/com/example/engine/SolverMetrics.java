package com.example.engine;

import java.util.Collections;
import java.util.List;

/**
 * Holds verification and complexity metrics produced by the Arrow Puzzle Solver.
 */
public final class SolverMetrics {
    private final boolean solvable;
    private final int totalArrows;
    private final int initialMovableCount;
    private final int dependencyWaves;
    private final List<Integer> solutionOrder;
    private final int complexityScore;

    public SolverMetrics(
            boolean solvable,
            int totalArrows,
            int initialMovableCount,
            int dependencyWaves,
            List<Integer> solutionOrder,
            int complexityScore
    ) {
        this.solvable = solvable;
        this.totalArrows = totalArrows;
        this.initialMovableCount = initialMovableCount;
        this.dependencyWaves = dependencyWaves;
        this.solutionOrder = solutionOrder != null
                ? Collections.unmodifiableList(solutionOrder)
                : Collections.emptyList();
        this.complexityScore = complexityScore;
    }

    public boolean isSolvable() {
        return solvable;
    }

    public int getTotalArrows() {
        return totalArrows;
    }

    public int getInitialMovableCount() {
        return initialMovableCount;
    }

    public int getDependencyWaves() {
        return dependencyWaves;
    }

    public List<Integer> getSolutionOrder() {
        return solutionOrder;
    }

    public int getComplexityScore() {
        return complexityScore;
    }
}
