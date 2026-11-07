package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ArrowPuzzleSolver
import com.example.engine.BoardThemePalette
import com.example.engine.Direction
import com.example.engine.PuzzleLevel
import com.example.engine.WindingArrow
import com.example.viewmodel.BlockedArrowFeedback
import com.example.viewmodel.EscapingArrowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.gestures.calculateCentroid
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AmazeBoardCanvas(
    level: PuzzleLevel,
    remainingArrows: List<WindingArrow>,
    escapingArrows: List<EscapingArrowState>,
    blockedFeedback: BlockedArrowFeedback?,
    hintedArrowId: Int?,
    themePalette: BoardThemePalette,
    zoomResetTrigger: Int,
    onArrowTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedPreviewArrowId: Int? = null,
    onArrowLongPressed: (Int) -> Unit = {},
    onArrowHoldReleased: (Int) -> Unit = {},
    onClearPreview: () -> Unit = {}
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val gestureScope = rememberCoroutineScope()

    LaunchedEffect(zoomResetTrigger, level.levelNumber, level.seed) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    var frameNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasActiveTimedAnims = escapingArrows.isNotEmpty() || blockedFeedback != null

    LaunchedEffect(hasActiveTimedAnims) {
        if (hasActiveTimedAnims) {
            while (isActive) {
                withFrameMillis {
                    frameNowMs = System.currentTimeMillis()
                }
            }
        }
    }

    val pulseFraction = if (hintedArrowId != null || selectedPreviewArrowId != null) {
        val infiniteTransition = rememberInfiniteTransition(label = "hintPulse")
        val anim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseFraction"
        )
        anim
    } else {
        0.5f
    }

    // Distinct, high-contrast Grid Dots underneath the arrows for easy visual tracking
    val visibleDotColor = if (themePalette.isDark) Color(0xFF8FA4C4) else Color(0xFF7D6247)
    val subGridDotColor = if (themePalette.isDark) Color(0xFF6B7F9E) else Color(0xFF9C8165)
    val primaryArrowColor = Color(themePalette.primaryArrowHex)
    val secondaryArrowColor = Color(themePalette.secondaryArrowHex)
    val highlightColor = Color(themePalette.highlightArrowHex)
    val blockedColor = Color(themePalette.blockedArrowHex)
    val trailParticleColor = Color(0xFF4CAF50)
    // Light, semi-transparent dotted black/charcoal line for path preview
    val dottedPathLineColor = if (themePalette.isDark) {
        Color(0xFFE8DCCB).copy(alpha = 0.46f)
    } else {
        Color(0xFF1C1C1C).copy(alpha = 0.40f)
    }
    val dottedPathShadowColor = if (themePalette.isDark) {
        Color(0xFFE8DCCB).copy(alpha = 0.07f)
    } else {
        Color(0xFF1C1C1C).copy(alpha = 0.06f)
    }

    val reusableStrokePath = remember { Path() }
    val reusableHeadPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("amaze_board_canvas")
            .pointerInput(level.levelNumber, level.seed, remainingArrows) {
                fun findArrowAtOffset(offset: Offset): WindingArrow? {
                    val cw = size.width.toFloat()
                    val ch = size.height.toFloat()
                    if (cw <= 1f || ch <= 1f) return null

                    val centerX = cw / 2f
                    val centerY = ch / 2f
                    val unzoomedX = (offset.x - centerX - panOffset.x) / zoomScale + centerX
                    val unzoomedY = (offset.y - centerY - panOffset.y) / zoomScale + centerY

                    val paddingPx = min(cw, ch) * 0.045f
                    val availW = cw - paddingPx * 2f
                    val availH = ch - paddingPx * 2f
                    val cellStep = min(
                        availW / (level.gridWidth - 1).coerceAtLeast(1),
                        availH / (level.gridHeight - 1).coerceAtLeast(1)
                    )
                    val boardW = cellStep * (level.gridWidth - 1)
                    val boardH = cellStep * (level.gridHeight - 1)
                    val originX = (cw - boardW) / 2f
                    val originY = (ch - boardH) / 2f

                    val gx = (unzoomedX - originX) / cellStep
                    val gy = (unzoomedY - originY) / cellStep
                    return findTappedArrow(gx, gy, remainingArrows)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    val hitArrow = findArrowAtOffset(downPos)

                    var isTransformGesture = false
                    var wasHoldTriggered = false
                    var maxPointersSeen = 1
                    var totalPanDistance = 0f

                    // Only trigger path preview if a single finger stays completely stationary for 480ms
                    val holdJob = if (hitArrow != null) {
                        gestureScope.launch {
                            delay(480L)
                            if (!isTransformGesture && maxPointersSeen == 1 && totalPanDistance < 6f) {
                                wasHoldTriggered = true
                                onArrowLongPressed(hitArrow.id)
                            }
                        }
                    } else null

                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount > maxPointersSeen) {
                            maxPointersSeen = pressedCount
                        }

                        if (pressedCount >= 2) {
                            if (!isTransformGesture) {
                                isTransformGesture = true
                                holdJob?.cancel()
                                if (wasHoldTriggered) {
                                    wasHoldTriggered = false
                                    onClearPreview()
                                }
                            }
                        }

                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (!isTransformGesture) {
                            totalPanDistance += panChange.getDistance()
                            // Any slight drag or pinch immediately switches to smooth pan/zoom and cancels long-press
                            if (totalPanDistance >= 6f || zoomChange != 1f) {
                                isTransformGesture = true
                                holdJob?.cancel()
                                if (wasHoldTriggered) {
                                    wasHoldTriggered = false
                                    onClearPreview()
                                }
                            }
                        }

                        if (isTransformGesture) {
                            val oldScale = zoomScale
                            val newScale = (oldScale * zoomChange).coerceIn(0.50f, 3.8f)
                            val actualZoomRatio = newScale / oldScale
                            zoomScale = newScale

                            val center = Offset(size.width / 2f, size.height / 2f)
                            val centroid = event.calculateCentroid(useCurrent = false)
                            val effectiveCentroid = if (centroid != Offset.Unspecified) centroid else center

                            // Smoothly zoom around the fingers' centroid and pan 1:1 with finger movement
                            val updatedPan = (panOffset + panChange) * actualZoomRatio +
                                (center - effectiveCentroid) * (actualZoomRatio - 1f)

                            val boundFactor = max(newScale, 1f) * 0.80f
                            val maxPanX = size.width * boundFactor
                            val maxPanY = size.height * boundFactor
                            panOffset = Offset(
                                x = updatedPan.x.coerceIn(-maxPanX, maxPanX),
                                y = updatedPan.y.coerceIn(-maxPanY, maxPanY)
                            )

                            event.changes.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    holdJob?.cancel()

                    if (wasHoldTriggered && hitArrow != null) {
                        onArrowHoldReleased(hitArrow.id)
                    } else if (!isTransformGesture && maxPointersSeen == 1 && totalPanDistance < 6f && hitArrow != null) {
                        onArrowTapped(hitArrow.id)
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            val paddingPx = min(size.width, size.height) * 0.045f
            val availW = size.width - paddingPx * 2f
            val availH = size.height - paddingPx * 2f

            val cellStep = min(
                availW / (level.gridWidth - 1).coerceAtLeast(1),
                availH / (level.gridHeight - 1).coerceAtLeast(1)
            )
            val boardW = cellStep * (level.gridWidth - 1)
            val boardH = cellStep * (level.gridHeight - 1)
            val originX = (size.width - boardW) / 2f
            val originY = (size.height - boardH) / 2f

            // Clip to the canvas viewport so nothing ever leaks into top/bottom HUD cards
            clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {
                withTransform({
                    translate(panOffset.x, panOffset.y)
                    scale(
                        scaleX = zoomScale,
                        scaleY = zoomScale,
                        pivot = Offset(size.width / 2f, size.height / 2f)
                    )
                }) {
                    // Also clip strictly to the board boundary (+ small margin for arrowheads)
                    // so escaping arrows vanish right as they exit the board and never cross into HUD bars!
                    val clipMargin = cellStep * 0.85f
                    clipRect(
                        left = originX - clipMargin,
                        top = originY - clipMargin,
                        right = originX + boardW + clipMargin,
                        bottom = originY + boardH + clipMargin
                    ) {
                        // 1. Draw clear, well-defined Dot Grid Matrix (نقاط الشبكة واضحة أسفل الأسهم)
                        // Primary grid vertex dots + corridor guide dots between lanes so the dot grid
                        // is always visible both when arrows cover vertices and after arrows exit!
                        val dotRadius = (cellStep * 0.135f).coerceIn(3.0f, 6.0f)
                        val subDotRadius = (cellStep * 0.075f).coerceIn(1.8f, 3.6f)
                        for (y in 0 until level.gridHeight) {
                            for (x in 0 until level.gridWidth) {
                                val px = originX + x * cellStep
                                val py = originY + y * cellStep
                                drawCircle(
                                    color = visibleDotColor.copy(alpha = 0.88f),
                                    radius = dotRadius,
                                    center = Offset(px, py)
                                )
                                if (x < level.gridWidth - 1 && y < level.gridHeight - 1) {
                                    drawCircle(
                                        color = subGridDotColor.copy(alpha = 0.55f),
                                        radius = subDotRadius,
                                        center = Offset(px + cellStep * 0.5f, py + cellStep * 0.5f)
                                    )
                                }
                            }
                        }

                        // 1.5 Draw Light, Completely Dotted Semi-Transparent Black Line (خط شفاف أسود جميل منقط كاملاً)
                        // ONLY when an arrow is long-pressed (auto-disappears after 3 seconds)
                        if (selectedPreviewArrowId != null) {
                            val previewArrow = remainingArrows.firstOrNull { it.id == selectedPreviewArrowId }
                            if (previewArrow != null) {
                                val headX = originX + previewArrow.head.x * cellStep
                                val headY = originY + previewArrow.head.y * cellStep
                                val dir = previewArrow.headDirection

                                val edgeGridX = when (dir) {
                                    Direction.LEFT -> -0.75f
                                    Direction.RIGHT -> level.gridWidth - 0.25f
                                    else -> previewArrow.head.x.toFloat()
                                }
                                val edgeGridY = when (dir) {
                                    Direction.UP -> -0.75f
                                    Direction.DOWN -> level.gridHeight - 0.25f
                                    else -> previewArrow.head.y.toFloat()
                                }

                                val edgeX = originX + edgeGridX * cellStep
                                val edgeY = originY + edgeGridY * cellStep

                                // Subtle soft lane shadow showing the arrow's corridor
                                drawLine(
                                    color = dottedPathShadowColor,
                                    start = Offset(headX, headY),
                                    end = Offset(edgeX, edgeY),
                                    strokeWidth = (cellStep * 0.34f).coerceAtLeast(6f),
                                    cap = StrokeCap.Round
                                )

                                // Light, completely dotted semi-transparent black guide line
                                drawLine(
                                    color = dottedPathLineColor,
                                    start = Offset(headX, headY),
                                    end = Offset(edgeX, edgeY),
                                    strokeWidth = (cellStep * 0.13f).coerceIn(2.2f, 4.5f),
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(cellStep * 0.14f, cellStep * 0.22f),
                                        0f
                                    )
                                )
                            }
                        }

                        // 2. Draw subtle dotted trace if an arrow just bumped into an obstacle
                        if (blockedFeedback != null) {
                            val blockedArrow = remainingArrows.firstOrNull { it.id == blockedFeedback.arrowId }
                            if (blockedArrow != null) {
                                val headX = originX + blockedArrow.head.x * cellStep
                                val headY = originY + blockedArrow.head.y * cellStep
                                val colX = originX + blockedFeedback.collisionPoint.x * cellStep
                                val colY = originY + blockedFeedback.collisionPoint.y * cellStep

                                drawLine(
                                    color = dottedPathLineColor,
                                    start = Offset(headX, headY),
                                    end = Offset(colX, colY),
                                    strokeWidth = (cellStep * 0.12f).coerceIn(2.0f, 4.0f),
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(cellStep * 0.14f, cellStep * 0.22f),
                                        0f
                                    )
                                )
                            }
                        }

                        // 3. Draw all active remaining arrows
                        for (arrow in remainingArrows) {
                            val isHinted = arrow.id == hintedArrowId
                            val isSelectedPreview = arrow.id == selectedPreviewArrowId
                            val isBlockedTarget = blockedFeedback?.arrowId == arrow.id
                            val isBlocker = blockedFeedback?.blockerArrowId == arrow.id

                            val bounceSteps = if (isBlockedTarget && blockedFeedback != null) {
                                val elapsed = (frameNowMs - blockedFeedback.triggeredAtMs).coerceIn(0L, 650L)
                                val t = elapsed / 650f
                                val maxDisplacement = min(
                                    blockedFeedback.clearStepsBeforeCollision + 0.36f,
                                    0.95f
                                )
                                (sin(t * Math.PI).toFloat() * maxDisplacement).coerceAtLeast(0f)
                            } else {
                                0f
                            }

                            val arrowColor = when {
                                isBlockedTarget -> blockedColor
                                isBlocker -> blockedColor.copy(alpha = 0.90f)
                                isHinted || isSelectedPreview -> highlightColor
                                arrow.colorAccentIndex == 1 -> highlightColor
                                arrow.id % 2 == 0 -> primaryArrowColor
                                else -> secondaryArrowColor
                            }

                            if (bounceSteps > 0.005f) {
                                val points = arrow.getInterpolatedPolyline(bounceSteps)
                                drawWindingArrowPolyline(
                                    gridPoints = points,
                                    headDirection = arrow.headDirection,
                                    originX = originX,
                                    originY = originY,
                                    cellStep = cellStep,
                                    color = arrowColor,
                                    isHighlighted = isHinted || isSelectedPreview || arrow.colorAccentIndex == 1,
                                    hasSparkleBadge = arrow.hasSparkleBadge,
                                    pulseFraction = pulseFraction,
                                    strokePath = reusableStrokePath,
                                    headPath = reusableHeadPath
                                )
                            } else {
                                drawStaticWindingArrow(
                                    arrow = arrow,
                                    originX = originX,
                                    originY = originY,
                                    cellStep = cellStep,
                                    color = arrowColor,
                                    isHighlighted = isHinted || isSelectedPreview || arrow.colorAccentIndex == 1,
                                    pulseFraction = pulseFraction,
                                    strokePath = reusableStrokePath,
                                    headPath = reusableHeadPath
                                )
                            }
                        }

                        // 4. Draw escaping arrows sliding along their winding path and vanishing at the board edge
                        for (escaping in escapingArrows) {
                            val elapsed = (frameNowMs - escaping.startedAtMs).coerceAtLeast(0L)
                            val durationMs = 380f
                            val rawProgress = (elapsed / durationMs).coerceIn(0f, 1f)
                            val eased = rawProgress * rawProgress * (3f - 2f * rawProgress)
                            val slideSteps = eased * escaping.totalStepsToClear

                            val poly = escaping.arrow.getInterpolatedPolyline(
                                slideSteps = slideSteps,
                                extraExitSteps = (escaping.totalStepsToClear.toInt() + 6)
                            )

                            if (poly.isNotEmpty()) {
                                val tailPt = poly.first()
                                for (k in 1..3) {
                                    val trailX = originX + (tailPt.first - escaping.arrow.headDirection.dx * k * 0.45f) * cellStep
                                    val trailY = originY + (tailPt.second - escaping.arrow.headDirection.dy * k * 0.45f) * cellStep
                                    drawCircle(
                                        color = trailParticleColor.copy(alpha = (1f - rawProgress) * (0.55f / k)),
                                        radius = dotRadius * 1.2f,
                                        center = Offset(trailX, trailY)
                                    )
                                }
                            }

                            drawWindingArrowPolyline(
                                gridPoints = poly,
                                headDirection = escaping.arrow.headDirection,
                                originX = originX,
                                originY = originY,
                                cellStep = cellStep,
                                color = highlightColor.copy(alpha = (1f - rawProgress * 0.65f).coerceIn(0f, 1f)),
                                isHighlighted = true,
                                hasSparkleBadge = false,
                                pulseFraction = 0.5f,
                                strokePath = reusableStrokePath,
                                headPath = reusableHeadPath
                            )
                        }
                    }
                }
            }
        }

        // Floating Quick Zoom & Pan Helper Pill (تقريب / تبعيد / توسيط بلمسة واحدة)
        val zoomPillBg = if (themePalette.isDark) Color(0xFF232B3B).copy(alpha = 0.90f) else Color(0xFFEDE0CA).copy(alpha = 0.92f)
        val zoomPillText = if (themePalette.isDark) Color(0xFFE8DCCB) else Color(0xFF53351A)

        Surface(
            shape = RoundedCornerShape(50),
            color = zoomPillBg,
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 8.dp)
                .border(1.dp, zoomPillText.copy(alpha = 0.15f), RoundedCornerShape(50))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clipToBounds()
                        .clickable {
                            zoomScale = (zoomScale + 0.25f).coerceAtMost(3.6f)
                        }
                        .testTag("zoom_in_button")
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = zoomPillText
                    )
                }

                Text(
                    text = "${(zoomScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = zoomPillText,
                    modifier = Modifier
                        .clickable {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clipToBounds()
                        .clickable {
                            val nextZoom = (zoomScale - 0.25f).coerceAtLeast(0.60f)
                            zoomScale = nextZoom
                            if (nextZoom <= 1.0f) {
                                panOffset = Offset.Zero
                            }
                        }
                        .testTag("zoom_out_button")
                ) {
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = zoomPillText
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawStaticWindingArrow(
    arrow: WindingArrow,
    originX: Float,
    originY: Float,
    cellStep: Float,
    color: Color,
    isHighlighted: Boolean,
    pulseFraction: Float,
    strokePath: Path,
    headPath: Path
) {
    val cells = arrow.cells
    if (cells.isEmpty()) return

    val strokeWidth = (cellStep * 0.29f).coerceIn(3.4f, 13.5f)
    strokePath.reset()

    val firstPx = originX + cells[0].x * cellStep
    val firstPy = originY + cells[0].y * cellStep
    strokePath.moveTo(firstPx, firstPy)

    for (i in 1 until cells.size) {
        val pt = cells[i]
        strokePath.lineTo(originX + pt.x * cellStep, originY + pt.y * cellStep)
    }

    val headPt = cells.last()
    val headX = originX + headPt.x * cellStep
    val headY = originY + headPt.y * cellStep

    finishDrawingArrowPaths(
        strokePath = strokePath,
        headPath = headPath,
        headX = headX,
        headY = headY,
        headDirection = arrow.headDirection,
        cellStep = cellStep,
        strokeWidth = strokeWidth,
        color = color,
        isHighlighted = isHighlighted,
        hasSparkleBadge = arrow.hasSparkleBadge,
        pulseFraction = pulseFraction
    )
}

private fun DrawScope.drawWindingArrowPolyline(
    gridPoints: List<Pair<Float, Float>>,
    headDirection: Direction,
    originX: Float,
    originY: Float,
    cellStep: Float,
    color: Color,
    isHighlighted: Boolean,
    hasSparkleBadge: Boolean,
    pulseFraction: Float,
    strokePath: Path,
    headPath: Path
) {
    if (gridPoints.isEmpty()) return

    val strokeWidth = (cellStep * 0.29f).coerceIn(3.4f, 13.5f)
    strokePath.reset()

    val firstPx = originX + gridPoints[0].first * cellStep
    val firstPy = originY + gridPoints[0].second * cellStep
    strokePath.moveTo(firstPx, firstPy)

    for (i in 1 until gridPoints.size) {
        val px = originX + gridPoints[i].first * cellStep
        val py = originY + gridPoints[i].second * cellStep
        strokePath.lineTo(px, py)
    }

    val headGrid = gridPoints.last()
    val headX = originX + headGrid.first * cellStep
    val headY = originY + headGrid.second * cellStep

    finishDrawingArrowPaths(
        strokePath = strokePath,
        headPath = headPath,
        headX = headX,
        headY = headY,
        headDirection = headDirection,
        cellStep = cellStep,
        strokeWidth = strokeWidth,
        color = color,
        isHighlighted = isHighlighted,
        hasSparkleBadge = hasSparkleBadge,
        pulseFraction = pulseFraction
    )
}

private fun DrawScope.finishDrawingArrowPaths(
    strokePath: Path,
    headPath: Path,
    headX: Float,
    headY: Float,
    headDirection: Direction,
    cellStep: Float,
    strokeWidth: Float,
    color: Color,
    isHighlighted: Boolean,
    hasSparkleBadge: Boolean,
    pulseFraction: Float
) {
    if (isHighlighted) {
        val glowStroke = strokeWidth * (1.75f + 0.30f * pulseFraction)
        drawPath(
            path = strokePath,
            color = color.copy(alpha = 0.24f + 0.14f * pulseFraction),
            style = Stroke(
                width = glowStroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    drawPath(
        path = strokePath,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    val tipAdvance = cellStep * 0.28f
    val backRetreat = cellStep * 0.18f
    val wingHalfWidth = (cellStep * 0.30f).coerceAtLeast(strokeWidth * 1.15f)

    val dx = headDirection.dx.toFloat()
    val dy = headDirection.dy.toFloat()
    val perpX = -dy
    val perpY = dx

    val tipX = headX + dx * tipAdvance
    val tipY = headY + dy * tipAdvance
    val baseCenterX = headX - dx * backRetreat
    val baseCenterY = headY - dy * backRetreat

    val leftWingX = baseCenterX + perpX * wingHalfWidth
    val leftWingY = baseCenterY + perpY * wingHalfWidth
    val rightWingX = baseCenterX - perpX * wingHalfWidth
    val rightWingY = baseCenterY - perpY * wingHalfWidth

    headPath.reset()
    headPath.moveTo(tipX, tipY)
    headPath.lineTo(leftWingX, leftWingY)
    headPath.lineTo(rightWingX, rightWingY)
    headPath.close()

    drawPath(
        path = headPath,
        color = color
    )

    if (hasSparkleBadge) {
        val badgeRadius = cellStep * (0.48f + 0.06f * pulseFraction)
        val sparkleGold = Color(0xFFFFEE58)
        val sparkleRing = Color(0xFFF3A833)
        drawCircle(
            color = sparkleGold.copy(alpha = 0.35f),
            radius = badgeRadius * 1.35f,
            center = Offset(headX, headY)
        )
        drawCircle(
            color = sparkleRing,
            radius = badgeRadius * 0.85f,
            center = Offset(headX, headY),
            style = Stroke(width = strokeWidth * 0.65f)
        )
        drawCircle(
            color = sparkleGold,
            radius = strokeWidth * 0.55f,
            center = Offset(headX - badgeRadius * 0.95f, headY - badgeRadius * 0.55f)
        )
        drawCircle(
            color = sparkleGold,
            radius = strokeWidth * 0.55f,
            center = Offset(headX - badgeRadius * 0.75f, headY + badgeRadius * 0.75f)
        )
        drawCircle(
            color = sparkleGold,
            radius = strokeWidth * 0.55f,
            center = Offset(headX + badgeRadius * 0.85f, headY - badgeRadius * 0.65f)
        )
    }
}

private fun findTappedArrow(
    gx: Float,
    gy: Float,
    arrows: List<WindingArrow>
): WindingArrow? {
    var bestArrow: WindingArrow? = null
    var bestDist = 0.68f

    for (arrow in arrows) {
        val cells = arrow.cells
        if (cells.isEmpty()) continue
        if (cells.size == 1) {
            val d = hypot(gx - cells[0].x, gy - cells[0].y)
            if (d < bestDist) {
                bestDist = d
                bestArrow = arrow
            }
            continue
        }

        for (i in 0 until cells.size - 1) {
            val a = cells[i]
            val b = cells[i + 1]
            val dist = distancePointToSegment(
                px = gx,
                py = gy,
                ax = a.x.toFloat(),
                ay = a.y.toFloat(),
                bx = b.x.toFloat(),
                by = b.y.toFloat()
            )
            val headBonus = if (i == cells.size - 2) 0.05f else 0f
            val effectiveDist = (dist - headBonus).coerceAtLeast(0f)
            if (effectiveDist < bestDist) {
                bestDist = effectiveDist
                bestArrow = arrow
            }
        }
    }

    return bestArrow
}

private fun distancePointToSegment(
    px: Float,
    py: Float,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float
): Float {
    val vx = bx - ax
    val vy = by - ay
    val wx = px - ax
    val wy = py - ay
    val lenSq = vx * vx + vy * vy
    if (lenSq <= 0.0001f) {
        return hypot(px - ax, py - ay)
    }
    val t = ((wx * vx + wy * vy) / lenSq).coerceIn(0f, 1f)
    val projX = ax + t * vx
    val projY = ay + t * vy
    return hypot(px - projX, py - projY)
}
