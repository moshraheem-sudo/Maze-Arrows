package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import com.example.engine.ArrowTipStyle
import com.example.engine.BoardThemePalette
import com.example.engine.Direction
import com.example.engine.MoveCheckResult
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
    arrowTipStyle: ArrowTipStyle = ArrowTipStyle.CLASSIC_TRIANGLE,
    eyeProtectionMode: Boolean = false,
    zoomScale: Float = 1f,
    onZoomScaleChange: (Float) -> Unit = {},
    onArrowLongPressed: (Int) -> Unit = {},
    onArrowHoldReleased: (Int) -> Unit = {},
    onClearPreview: () -> Unit = {}
) {
    var internalZoomScale by remember { mutableFloatStateOf(zoomScale) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val gestureScope = rememberCoroutineScope()

    LaunchedEffect(zoomScale) {
        if (internalZoomScale != zoomScale) {
            internalZoomScale = zoomScale
            if (zoomScale <= 1.0f) {
                panOffset = Offset.Zero
            }
        }
    }

    LaunchedEffect(zoomResetTrigger, level.levelNumber, level.seed) {
        internalZoomScale = 1f
        onZoomScaleChange(1f)
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

    val infiniteTransition = rememberInfiniteTransition(label = "laserPathTransition")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseFraction"
    )
    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashOffset"
    )
    val laserPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPulseAlpha"
    )

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
                    val unzoomedX = (offset.x - centerX - panOffset.x) / internalZoomScale + centerX
                    val unzoomedY = (offset.y - centerY - panOffset.y) / internalZoomScale + centerY

                    val paddingPx = min(cw, ch) * 0.025f
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

                    // Only trigger path preview if a single finger stays stationary for 320ms (smooth long-press)
                    val holdJob = if (hitArrow != null) {
                        gestureScope.launch {
                            delay(320L)
                            if (!isTransformGesture && maxPointersSeen == 1 && totalPanDistance < 8f) {
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
                            val oldScale = internalZoomScale
                            val newScale = (oldScale * zoomChange).coerceIn(0.50f, 3.8f)
                            val actualZoomRatio = newScale / oldScale
                            internalZoomScale = newScale
                            onZoomScaleChange(newScale)

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
            val paddingPx = min(size.width, size.height) * 0.025f
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
                        scaleX = internalZoomScale,
                        scaleY = internalZoomScale,
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



                        // 2. Draw subtle dotted trace if an arrow just bumped into an obstacle
                        if (blockedFeedback != null) {
                            val blockedArrow = remainingArrows.firstOrNull { it.id == blockedFeedback.arrowId }
                            if (blockedArrow != null) {
                                val headX = originX + blockedArrow.head.x * cellStep
                                val headY = originY + blockedArrow.head.y * cellStep
                                val colX = originX + blockedFeedback.collisionPoint.x * cellStep
                                val colY = originY + blockedFeedback.collisionPoint.y * cellStep

                                drawLine(
                                    color = Color(0xFFFF1744).copy(alpha = 0.85f),
                                    start = Offset(headX, headY),
                                    end = Offset(colX, colY),
                                    strokeWidth = (cellStep * 0.15f).coerceIn(3.0f, 6.0f),
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(cellStep * 0.16f, cellStep * 0.14f),
                                        0f
                                    )
                                )
                            }
                        }

                        // 3. Draw all active remaining arrows
                        for (arrow in remainingArrows) {
                            val isHinted = arrow.id == hintedArrowId
                            val isSelectedPreview = arrow.id == selectedPreviewArrowId
                            val isInspectedOrHinted = isHinted || isSelectedPreview
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
                                isInspectedOrHinted -> Color(0xFFFF1744) // Bright Neon Red for Hint & Preview!
                                arrow.colorAccentIndex == 1 -> highlightColor
                                else -> primaryArrowColor
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
                                    arrowTipStyle = arrowTipStyle,
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
                                    arrowTipStyle = arrowTipStyle,
                                    strokePath = reusableStrokePath,
                                    headPath = reusableHeadPath
                                )
                            }
                        }

                        // 3.5 Draw Ultra-Thin Smooth Red Laser Beam Penetrating Across Entire Grid
                        // (خط مسار ناعم مثل الليزر يخترق الشبكة بأكملها ليرى المستخدم المسار الكامل للسهم - رفيع أحمر وبشكل مميز)
                        val previewTargetArrowId = selectedPreviewArrowId ?: hintedArrowId
                        if (previewTargetArrowId != null) {
                            val previewArrow = remainingArrows.firstOrNull { it.id == previewTargetArrowId }
                            if (previewArrow != null) {
                                val headX = originX + previewArrow.head.x * cellStep
                                val headY = originY + previewArrow.head.y * cellStep
                                val dir = previewArrow.headDirection

                                // Laser penetrates the entire grid from arrow head all the way past outer board edge
                                val fullLaserEndX = when (dir) {
                                    Direction.LEFT -> originX - clipMargin * 0.70f
                                    Direction.RIGHT -> originX + boardW + clipMargin * 0.70f
                                    Direction.UP, Direction.DOWN -> headX
                                }
                                val fullLaserEndY = when (dir) {
                                    Direction.UP -> originY - clipMargin * 0.70f
                                    Direction.DOWN -> originY + boardH + clipMargin * 0.70f
                                    Direction.LEFT, Direction.RIGHT -> headY
                                }

                                val moveResult = ArrowPuzzleSolver.checkMove(
                                    level.gridWidth,
                                    level.gridHeight,
                                    remainingArrows,
                                    previewArrow
                                )

                                val tipAdvance = cellStep * 0.35f
                                val laserStartX = headX + dir.dx * tipAdvance
                                val laserStartY = headY + dir.dy * tipAdvance
                                val startLaserOffset = Offset(laserStartX, laserStartY)
                                val endLaserOffset = Offset(fullLaserEndX, fullLaserEndY)

                                // Layer 1: Ambient Red Laser Bloom / Glow (هالة ليزرية رقيقة وناعمة)
                                drawLine(
                                    color = Color(0xFFFF1744).copy(alpha = 0.22f * laserPulseAlpha),
                                    start = startLaserOffset,
                                    end = endLaserOffset,
                                    strokeWidth = 3.0f,
                                    cap = StrokeCap.Round
                                )

                                // Layer 2: Main Vivid Crimson Laser Beam (شعاع الليزر الأحمر الصريح والرفيع جداً)
                                drawLine(
                                    color = Color(0xFFFF0D36),
                                    start = startLaserOffset,
                                    end = endLaserOffset,
                                    strokeWidth = 1.4f,
                                    cap = StrokeCap.Round
                                )

                                // Layer 3: High-Energy Photonic Core (قلب الليزر الأبيض المشع فائق الدقة)
                                drawLine(
                                    color = Color(0xFFFFF0F5).copy(alpha = 0.90f),
                                    start = startLaserOffset,
                                    end = endLaserOffset,
                                    strokeWidth = 0.7f,
                                    cap = StrokeCap.Round
                                )

                                // Layer 4: Subtle energetic laser pulses traveling along beam
                                drawLine(
                                    color = Color(0xFFFF8A80).copy(alpha = 0.65f),
                                    start = startLaserOffset,
                                    end = endLaserOffset,
                                    strokeWidth = 1.0f,
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(cellStep * 0.18f, cellStep * 0.22f),
                                        -dashOffset
                                    )
                                )

                                // Laser Emitter Node at Arrow Head Tip
                                drawCircle(
                                    color = Color(0xFFFF1744).copy(alpha = 0.35f * laserPulseAlpha),
                                    radius = 3.5f,
                                    center = startLaserOffset
                                )
                                drawCircle(
                                    color = Color(0xFFFF0D36),
                                    radius = 2.2f,
                                    center = startLaserOffset
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 1.0f,
                                    center = startLaserOffset
                                )

                                // Laser Obstacle Collision or Clear Exit Feedback along the beam
                                when (moveResult) {
                                    is MoveCheckResult.Clear -> {
                                        // Clear path: sleek luminous emerald dot at board boundary
                                        drawCircle(
                                            color = Color(0xFF00E676).copy(alpha = 0.35f * laserPulseAlpha),
                                            radius = 5.0f,
                                            center = endLaserOffset
                                        )
                                        drawCircle(
                                            color = Color(0xFF00E676),
                                            radius = 2.8f,
                                            center = endLaserOffset
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 1.2f,
                                            center = endLaserOffset
                                        )
                                    }
                                    is MoveCheckResult.Blocked -> {
                                        // Blocked path: sleek impact reticle where collision happens
                                        val colX = originX + moveResult.collisionPoint.x.toFloat() * cellStep
                                        val colY = originY + moveResult.collisionPoint.y.toFloat() * cellStep
                                        val colOffset = Offset(colX, colY)

                                        drawCircle(
                                            color = Color(0xFFFF1744).copy(alpha = 0.25f * laserPulseAlpha),
                                            radius = 6.0f * (1f + 0.1f * pulseFraction),
                                            center = colOffset
                                        )
                                        drawCircle(
                                            color = Color(0xFFFF1744),
                                            radius = 3.8f,
                                            center = colOffset,
                                            style = Stroke(width = 1.2f)
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 1.4f,
                                            center = colOffset
                                        )
                                    }
                                }
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
                                arrowTipStyle = arrowTipStyle,
                                strokePath = reusableStrokePath,
                                headPath = reusableHeadPath
                            )
                        }
                    }
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
    arrowTipStyle: ArrowTipStyle,
    strokePath: Path,
    headPath: Path
) {
    val cells = arrow.cells
    if (cells.isEmpty()) return

    val strokeWidth = (cellStep * 0.48f).coerceIn(4.8f, 18.0f)
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
        pulseFraction = pulseFraction,
        arrowTipStyle = arrowTipStyle
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
    arrowTipStyle: ArrowTipStyle,
    strokePath: Path,
    headPath: Path
) {
    if (gridPoints.isEmpty()) return

    val strokeWidth = (cellStep * 0.48f).coerceIn(4.8f, 18.0f)
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
        pulseFraction = pulseFraction,
        arrowTipStyle = arrowTipStyle
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
    pulseFraction: Float,
    arrowTipStyle: ArrowTipStyle = ArrowTipStyle.CLASSIC_TRIANGLE
) {
    drawPath(
        path = strokePath,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    val tipAdvance = cellStep * 0.34f
    val backRetreat = cellStep * 0.20f
    val wingHalfWidth = (cellStep * 0.40f).coerceAtLeast(strokeWidth * 0.90f)

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
    when (arrowTipStyle) {
        ArrowTipStyle.CLASSIC_TRIANGLE -> {
            headPath.moveTo(tipX, tipY)
            headPath.lineTo(leftWingX, leftWingY)
            headPath.lineTo(rightWingX, rightWingY)
            headPath.close()
        }
        ArrowTipStyle.STEALTH_CHEVRON -> {
            val notchX = baseCenterX + dx * (backRetreat * 0.75f)
            val notchY = baseCenterY + dy * (backRetreat * 0.75f)
            headPath.moveTo(tipX, tipY)
            headPath.lineTo(leftWingX, leftWingY)
            headPath.lineTo(notchX, notchY)
            headPath.lineTo(rightWingX, rightWingY)
            headPath.close()
        }
        ArrowTipStyle.DIAMOND_HEAD -> {
            val diamondBackX = headX - dx * (backRetreat * 1.4f)
            val diamondBackY = headY - dy * (backRetreat * 1.4f)
            headPath.moveTo(tipX, tipY)
            headPath.lineTo(leftWingX, leftWingY)
            headPath.lineTo(diamondBackX, diamondBackY)
            headPath.lineTo(rightWingX, rightWingY)
            headPath.close()
        }
        ArrowTipStyle.ROUNDED_SHIELD -> {
            val ctrlAdvX = tipX - dx * (tipAdvance * 0.35f)
            val ctrlAdvY = tipY - dy * (tipAdvance * 0.35f)
            headPath.moveTo(tipX, tipY)
            headPath.quadraticTo(ctrlAdvX + perpX * (wingHalfWidth * 0.85f), ctrlAdvY + perpY * (wingHalfWidth * 0.85f), leftWingX, leftWingY)
            headPath.lineTo(rightWingX, rightWingY)
            headPath.quadraticTo(ctrlAdvX - perpX * (wingHalfWidth * 0.85f), ctrlAdvY - perpY * (wingHalfWidth * 0.85f), tipX, tipY)
            headPath.close()
        }
        ArrowTipStyle.HARPOON_BARB -> {
            val barbBackX = baseCenterX - dx * (backRetreat * 0.6f)
            val barbBackY = baseCenterY - dy * (backRetreat * 0.6f)
            val barbLeftX = barbBackX + perpX * (wingHalfWidth * 1.25f)
            val barbLeftY = barbBackY + perpY * (wingHalfWidth * 1.25f)
            val barbRightX = barbBackX - perpX * (wingHalfWidth * 1.25f)
            val barbRightY = barbBackY - perpY * (wingHalfWidth * 1.25f)
            headPath.moveTo(tipX, tipY)
            headPath.lineTo(barbLeftX, barbLeftY)
            headPath.lineTo(baseCenterX + perpX * (wingHalfWidth * 0.35f), baseCenterY + perpY * (wingHalfWidth * 0.35f))
            headPath.lineTo(baseCenterX - perpX * (wingHalfWidth * 0.35f), baseCenterY - perpY * (wingHalfWidth * 0.35f))
            headPath.lineTo(barbRightX, barbRightY)
            headPath.close()
        }
    }

    drawPath(
        path = headPath,
        color = color
    )
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
