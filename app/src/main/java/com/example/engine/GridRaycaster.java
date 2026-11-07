package com.example.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance 2D grid raycasting and collision utility written in Java.
 * Used by both the real-time Game Engine and the Puzzle Solver / Level Generator.
 */
public final class GridRaycaster {

    private GridRaycaster() {
        // Utility class
    }

    /**
     * Checks if the straight exit ray from (headX, headY) in direction (dx, dy) is completely clear.
     *
     * @param width        Grid width
     * @param height       Grid height
     * @param occupiedGrid Flattened array of size width * height containing arrow IDs (-1 for empty)
     * @param headX        Arrowhead X coordinate
     * @param headY        Arrowhead Y coordinate
     * @param dx           Direction step X (-1, 0, or 1)
     * @param dy           Direction step Y (-1, 0, or 1)
     * @param selfArrowId  ID of the arrow being tested
     * @return -1 if the ray to the board edge is completely clear, or the ID of the first blocking arrow
     */
    public static int findFirstBlocker(
            int width,
            int height,
            int[] occupiedGrid,
            int headX,
            int headY,
            int dx,
            int dy,
            int selfArrowId
    ) {
        int cx = headX + dx;
        int cy = headY + dy;
        while (cx >= 0 && cx < width && cy >= 0 && cy < height) {
            int occupant = occupiedGrid[cy * width + cx];
            if (occupant != -1 && occupant != selfArrowId) {
                return occupant;
            }
            if (occupant == selfArrowId) {
                return selfArrowId;
            }
            cx += dx;
            cy += dy;
        }
        return -1;
    }

    /**
     * Counts how many empty grid cells exist directly in front of (headX, headY)
     * before hitting a blocking arrow or exiting the board.
     */
    public static int countClearSteps(
            int width,
            int height,
            int[] occupiedGrid,
            int headX,
            int headY,
            int dx,
            int dy,
            int selfArrowId
    ) {
        int steps = 0;
        int cx = headX + dx;
        int cy = headY + dy;
        while (cx >= 0 && cx < width && cy >= 0 && cy < height) {
            int occupant = occupiedGrid[cy * width + cx];
            if (occupant != -1) {
                return steps;
            }
            steps++;
            cx += dx;
            cy += dy;
        }
        return steps;
    }

    /**
     * Returns packed (y * width + x) cell indices along the exit ray from (headX, headY)
     * in direction (dx, dy) until the board boundary.
     */
    public static List<Integer> getExitRayIndices(
            int width,
            int height,
            int headX,
            int headY,
            int dx,
            int dy
    ) {
        List<Integer> ray = new ArrayList<>();
        int cx = headX + dx;
        int cy = headY + dy;
        while (cx >= 0 && cx < width && cy >= 0 && cy < height) {
            ray.add(cy * width + cx);
            cx += dx;
            cy += dy;
        }
        return ray;
    }
}
