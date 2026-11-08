package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import com.example.engine.ArrowTipStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GameRepository
import com.example.data.PlayerProfileEntity
import com.example.engine.AppThemeMode
import com.example.engine.BoardThemePalette
import com.example.engine.DifficultyTier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.text.style.TextOverflow
import com.example.security.SecurityAuditReport
import com.example.security.SecurityShieldManager
import com.example.update.GitHubAppUpdater
import com.example.update.GitHubUpdateCheckState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun formatTimerMmSs(seconds: Int): String {
    val clamped = seconds.coerceAtLeast(0)
    val mins = clamped / 60
    val secs = clamped % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
fun AmazeGameplayTopBar(
    levelNumber: Int,
    highestUnlockedLevel: Int,
    isCustomLevel: Boolean,
    difficulty: DifficultyTier,
    themePalette: BoardThemePalette,
    totalScore: Int = 0,
    isModeLocked: Boolean = false,
    onBackClick: () -> Unit,
    onPreviousLevelClick: () -> Unit = {},
    onNextUnlockedLevelClick: () -> Unit = {},
    onShopClick: () -> Unit = {},
    onThemeClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val barBgColor = if (themePalette.isDark) Color(0xFF1D2331) else Color(0xFFEDE0CA)
    val iconSurfaceColor = if (themePalette.isDark) Color(0xFF283042) else Color(0xFFF7EFE2)
    val textColor = if (themePalette.isDark) Color(0xFFE8DCCB) else Color(0xFF5A3B1E)
    val titleColor = if (themePalette.isDark) Color(0xFFFFB74D) else Color(0xFF8C4A15)
    val diffBadgeColor = Color(difficulty.badgeColorHex)

    Surface(
        color = barBgColor,
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Pinned App Name at Top-Center (تثبيت اسم التطبيق Maze Arrows في أعلى المنتصف)
            Text(
                text = "Maze Arrows",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                ),
                maxLines = 1,
                color = titleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            // 2. Second Row: Back Button + Level/Mode Info + Spaced Shop/Theme/Settings Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = iconSurfaceColor,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("back_to_menu_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = textColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Center: Level Name + Compact Difficulty Badge (never clips or disappears!)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (isCustomLevel) "مخصص" else "المستوى $levelNumber",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = diffBadgeColor.copy(alpha = if (themePalette.isDark) 0.32f else 0.16f),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = if (themePalette.isDark) Color(0xFFFFCC80).copy(alpha = 0.65f) else diffBadgeColor.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50)
                        )
                    ) {
                        Text(
                            text = if (isModeLocked) "🔒${difficulty.arabicLabel}" else difficulty.arabicLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            color = if (themePalette.isDark) Color(0xFFFFE082) else diffBadgeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // Side Actions (Shop + Theme + Settings)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Points Shop Button
                    Surface(
                        onClick = onShopClick,
                        shape = RoundedCornerShape(50),
                        color = iconSurfaceColor,
                        modifier = Modifier.testTag("gameplay_shop_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "متجر النقاط",
                                tint = if (themePalette.isDark) Color(0xFFFFCA28) else Color(0xFFD9822B),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$totalScore",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                color = textColor
                            )
                        }
                    }

                    Surface(
                        onClick = onThemeClick,
                        shape = CircleShape,
                        color = iconSurfaceColor,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("theme_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "تغيير المظهر",
                                tint = textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onSettingsClick,
                        shape = CircleShape,
                        color = iconSurfaceColor,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmazeGameplaySubBar(
    livesRemaining: Int,
    maxLives: Int,
    remainingSeconds: Int,
    remainingArrowsCount: Int,
    totalArrowsCount: Int,
    comboCount: Int,
    hintsBalance: Int,
    themePalette: BoardThemePalette,
    onHintClick: () -> Unit
) {
    val cardBgColor = if (themePalette.isDark) Color(0xFF232B3B) else Color(0xFFF2E6D4)
    val pillBgColor = if (themePalette.isDark) Color(0xFF1A202C) else Color(0xFFE7D7C0)
    val textColor = if (themePalette.isDark) Color(0xFFE8DCCB) else Color(0xFF53351A)
    val isLowTime = remainingSeconds <= 20

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBgColor,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                color = textColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 5 Attempts (المحاولات الخمس)
            Surface(
                shape = RoundedCornerShape(50),
                color = pillBgColor
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("lives_indicator")
                ) {
                    for (i in 1..maxLives) {
                        val isFilled = i <= livesRemaining
                        WaterDropletIcon(
                            isFilled = isFilled,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$livesRemaining/$maxLives محاولة",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        color = textColor
                    )
                }
            }

            // Live Countdown Timer Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isLowTime) {
                    Color(0xFFC62828).copy(alpha = 0.18f)
                } else {
                    pillBgColor
                },
                modifier = Modifier.testTag("level_timer_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "المؤقت",
                        tint = if (isLowTime) Color(0xFFC62828) else Color(0xFFB36A22),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = formatTimerMmSs(remainingSeconds),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        color = if (isLowTime) Color(0xFFC62828) else textColor
                    )
                }
            }

            // Remaining Arrows Counter + Combo indicator Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = pillBgColor
                ) {
                    Text(
                        text = "$remainingArrowsCount/$totalArrowsCount سهم",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                AnimatedVisibility(
                    visible = comboCount >= 2,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF3A833)
                    ) {
                        Text(
                            text = "×$comboCount",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                            maxLines = 1,
                            color = Color(0xFF4E3117),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaterDropletIcon(
    isFilled: Boolean,
    modifier: Modifier = Modifier
) {
    val activeBlue = Color(0xFF4AA3F0)
    val emptyGray = Color(0xFFC2B7A7)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dropletPath = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            cubicTo(
                w * 0.12f, h * 0.52f,
                w * 0.14f, h * 0.92f,
                w * 0.5f, h * 0.94f
            )
            cubicTo(
                w * 0.86f, h * 0.92f,
                w * 0.88f, h * 0.52f,
                w * 0.5f, h * 0.06f
            )
            close()
        }
        drawPath(
            path = dropletPath,
            color = if (isFilled) activeBlue else emptyGray
        )
    }
}

@Composable
fun AmazeFloatingToastBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + scaleIn(initialScale = 0.90f),
        exit = fadeOut() + scaleOut(targetScale = 0.90f),
        modifier = modifier
    ) {
        val isRedPathAlert = message?.contains("أحمر") == true ||
            message?.contains("تلميح") == true ||
            message?.contains("مسار") == true ||
            message?.contains("✅") == true ||
            message?.contains("⛔") == true

        val bgColor = if (isRedPathAlert) Color(0xFF111827) else Color(0xFFFFFDF9).copy(alpha = 0.96f)
        val borderColor = if (isRedPathAlert) Color(0xFFFF1744) else Color(0xFFD9822B).copy(alpha = 0.40f)
        val textColor = if (isRedPathAlert) Color(0xFFFFFFFF) else Color(0xFF8C4A15)

        Surface(
            shape = RoundedCornerShape(50),
            color = bgColor,
            shadowElevation = 5.dp,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .widthIn(max = 380.dp)
                .border(
                    width = if (isRedPathAlert) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(50)
                )
                .testTag("floating_toast_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isRedPathAlert) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFFF1744), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                }
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AmazeGameplayBottomBar(
    canUndo: Boolean,
    previewModeEnabled: Boolean,
    isAutoSolving: Boolean,
    themePalette: BoardThemePalette,
    onUndoClick: () -> Unit,
    onHintClick: () -> Unit,
    onTogglePreviewMode: () -> Unit,
    onAutoSolveToggle: () -> Unit,
    onResetZoomClick: () -> Unit,
    pathInspectCooldownSeconds: Int = 0,
    hintCooldownSeconds: Int = 0,
    autoSolveCooldownSeconds: Int = 0
) {
    val isDark = themePalette.isDark
    val cardBg = if (isDark) Color(0xFF20293C) else Color(0xFFF4E8D6)
    val chipBg = if (isDark) Color(0xFF2B3750) else Color(0xFFEBDAC2)
    val chipBorder = if (isDark) Color(0xFF47587A) else Color(0xFFD7C2A6)
    val contentTint = if (isDark) Color(0xFFFFFFFF) else Color(0xFF3E240E)
    val isHintLocked = hintCooldownSeconds > 0
    val isPathInspectLocked = pathInspectCooldownSeconds > 0
    val isAutoSolveLocked = autoSolveCooldownSeconds > 0 && !isAutoSolving

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF3E4E6E) else Color(0xFFE0CEB4),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomActionChip(
                icon = Icons.AutoMirrored.Filled.Undo,
                label = "تراجع",
                enabled = canUndo,
                tint = if (canUndo) contentTint else if (isDark) Color(0xFFCFD8DC) else Color(0xFF8D745C),
                chipBg = chipBg,
                chipBorder = chipBorder,
                testTag = "undo_button",
                onClick = onUndoClick
            )

            BottomActionChip(
                icon = Icons.Default.Lightbulb,
                label = if (isHintLocked) "🔒 ${formatTimerMmSs(hintCooldownSeconds)}" else "تلميح",
                enabled = true,
                tint = when {
                    isHintLocked -> if (isDark) Color(0xFFFFECB3) else Color(0xFF8C5820)
                    isDark -> Color(0xFFFFD54F)
                    else -> Color(0xFFC86D14)
                },
                chipBg = chipBg,
                chipBorder = chipBorder,
                testTag = "bottom_hint_button",
                onClick = onHintClick
            )

            BottomActionChip(
                icon = Icons.Default.Visibility,
                label = if (isPathInspectLocked) "🔒 ${formatTimerMmSs(pathInspectCooldownSeconds)}" else "فحص المسار",
                enabled = true,
                tint = when {
                    isPathInspectLocked -> if (isDark) Color(0xFFB3E5FC) else Color(0xFF6B533B)
                    isDark -> Color(0xFF80D8FF)
                    else -> contentTint
                },
                chipBg = chipBg,
                chipBorder = chipBorder,
                testTag = "preview_mode_button",
                onClick = onTogglePreviewMode
            )

            BottomActionChip(
                icon = if (isAutoSolving) Icons.Default.Close else Icons.Default.AutoAwesome,
                label = when {
                    isAutoSolving -> "إيقاف"
                    isAutoSolveLocked -> "🔒 ${formatTimerMmSs(autoSolveCooldownSeconds)}"
                    else -> "حل تلقائي"
                },
                enabled = true,
                tint = when {
                    isAutoSolving -> if (isDark) Color(0xFFFF8A80) else Color(0xFFD32F2F)
                    isAutoSolveLocked -> if (isDark) Color(0xFFE1BEE7) else Color(0xFF6A3B7D)
                    isDark -> Color(0xFFEA80FC)
                    else -> Color(0xFF6A1B9A)
                },
                chipBg = chipBg,
                chipBorder = chipBorder,
                testTag = "auto_solve_button",
                onClick = onAutoSolveToggle
            )

            BottomActionChip(
                icon = Icons.Default.CenterFocusStrong,
                label = "ملاءمة",
                enabled = true,
                tint = contentTint,
                chipBg = chipBg,
                chipBorder = chipBorder,
                testTag = "reset_zoom_button",
                onClick = onResetZoomClick
            )
        }
    }
}

@Composable
private fun BottomActionChip(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color,
    chipBg: Color,
    chipBorder: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(chipBg)
            .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            maxLines = 1,
            softWrap = false,
            color = tint
        )
    }
}

@Composable
fun AmazeSettingsDialog(
    profile: PlayerProfileEntity,
    showRestartButton: Boolean,
    themePalette: BoardThemePalette = BoardThemePalette.AMAZE_CLASSIC,
    updateCheckState: GitHubUpdateCheckState = GitHubUpdateCheckState(),
    onCheckGitHubUpdate: () -> Unit = {},
    onDownloadGitHubUpdate: () -> Unit = {},
    onDismiss: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleVoice: (Boolean) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit = {},
    onSelectArrowTipStyle: (ArrowTipStyle) -> Unit = {},
    onToggleEyeProtection: (Boolean) -> Unit = {},
    screenPrivacyProtection: Boolean = false,
    onToggleScreenPrivacyProtection: (Boolean) -> Unit = {},
    onOpenSecurityShieldDialog: () -> Unit = {},
    onRestartLevel: () -> Unit
) {
    val activeThemeMode = AppThemeMode.fromId(profile.themeMode)
    val activeArrowTipStyle = ArrowTipStyle.fromId(profile.arrowTipStyle)
    val isDark = themePalette.isDark || activeThemeMode == AppThemeMode.DARK

    val dialogBg = if (isDark) Color(0xFF131822) else Color(0xFFFAF3E6)
    val topBarBg = if (isDark) Color(0xFF1C2433) else Color(0xFFEDE0CA)
    val cardBg = if (isDark) Color(0xFF222B3D) else Color(0xFFF1E4CE)
    val subCardBg = if (isDark) Color(0xFF18202E) else Color(0xFFFAF3E6)
    val cardBorder = if (isDark) Color(0xFF384661) else Color(0xFFD7C6B0)
    val titleColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF2B1B0E)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF4E3117)
    val subTextColor = if (isDark) Color(0xFFAEBCCF) else Color(0xFF6B4624)
    val brandAccent = if (isDark) Color(0xFFFFB74D) else Color(0xFFB36A22)
    val dividerColor = if (isDark) Color(0xFF384661) else Color(0xFFD7C6B0).copy(alpha = 0.5f)
    val closeBtnBg = if (isDark) Color(0xFF2B364C) else Color(0xFFFAF3E6)
    val closeBtnTint = if (isDark) Color(0xFFE8DCCB) else Color(0xFF53351A)

    // Full-screen Settings window across the entire app level, respecting system status & navigation bars
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            color = dialogBg,
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Full-width Pinned Top Bar for Settings
                Surface(
                    color = topBarBg,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = "إعدادات التطبيق الكاملة",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                ),
                                color = titleColor,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Maze Arrows • الإصدار ${updateCheckState.installedVersionName}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = brandAccent,
                                textAlign = TextAlign.Center
                            )
                        }
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = closeBtnBg,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(40.dp)
                                .testTag("close_settings_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = closeBtnTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Scrollable Full-Screen Content with generous bottom clearance above navigation bar
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // =========================================================================
                    // القسم 1: مبدل الثيمات (Theme Mode) — منصف في منتصف الشاشة ومتوافق تماماً
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = brandAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مبدل الثيمات (Theme Mode)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    ),
                                    color = titleColor,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val themeOptions = listOf(
                                    Triple(AppThemeMode.LIGHT, "الثيم الفاتح", "Light Mode"),
                                    Triple(AppThemeMode.DARK, "الثيم الداكن", "Dark Mode"),
                                    Triple(AppThemeMode.SYSTEM, "الثيم الافتراضي للنظام", "System Default")
                                )
                                for ((mode, arTitle, enSub) in themeOptions) {
                                    val isSelected = mode == activeThemeMode
                                    Surface(
                                        onClick = { onSelectThemeMode(mode) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) brandAccent else subCardBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) brandAccent else cardBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .testTag("theme_mode_${mode.id.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = "$arTitle ($enSub)",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isSelected) Color.White else textColor,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // =========================================================================
                    // القسم 2: شكل ورأس السهم (Arrow Tip Style Customization)
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = brandAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شكل ورأس السهم في المتاهة",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    ),
                                    color = titleColor,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = "اختر شكل رأس السهم المفضل لديك لتحسين دقة وسرعة الرؤية أثناء اللعب",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = subTextColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (style in ArrowTipStyle.entries) {
                                    val isSelected = style == activeArrowTipStyle
                                    Surface(
                                        onClick = { onSelectArrowTipStyle(style) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) brandAccent.copy(alpha = if (isDark) 0.32f else 0.18f) else subCardBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) brandAccent else cardBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .testTag("arrow_style_${style.name.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isDark) Color(0xFF2B364C) else Color(0xFFE9DCBA),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Navigation,
                                                            contentDescription = null,
                                                            tint = if (isSelected) brandAccent else textColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = style.arabicName,
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        ),
                                                        color = titleColor
                                                    )
                                                    Text(
                                                        text = style.descriptionAr,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                                        color = subTextColor
                                                    )
                                                }
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "محدد",
                                                    tint = brandAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // =========================================================================
                    // القسم 3: وضع حماية البصر وقاعدة 20-20-20 (Eye Comfort Protection)
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveRedEye,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "وضع حماية البصر (Eye Comfort)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    ),
                                    color = titleColor,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "فلتر الضوء المريح للعينين",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = titleColor
                                    )
                                    Text(
                                        text = "تدرجات لونية هادئة تقلل من التوهج وإجهاد البصر أثناء التركيز وحل المتاهات",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = subTextColor
                                    )
                                }

                                Switch(
                                    checked = profile.eyeProtectionMode,
                                    onCheckedChange = onToggleEyeProtection,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF2E7D32),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = cardBorder
                                    ),
                                    modifier = Modifier.testTag("toggle_eye_protection")
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = subCardBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "تذكير قاعدة 20-20-20 مفعل تلقائياً: إذا استمر تركيزك لأكثر من 20 دقيقة، سينبهك التطبيق للنظر بعيداً 20 ثانية لإراحة عضلات العينين.",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // =========================================================================
                    // القسم 4: إعدادات الصوت والاهتزاز (Audio & Haptics) — مجموعة منسقة ومنصفة
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "إعدادات الصوت والاهتزاز والتفاعل",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                ),
                                color = titleColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SettingsToggleRow(
                                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                                label = "المؤثرات الصوتية",
                                checked = profile.soundEnabled,
                                onCheckedChange = onToggleSound,
                                testTag = "toggle_sound",
                                labelColor = textColor,
                                iconTint = brandAccent
                            )
                            HorizontalDivider(color = dividerColor)
                            SettingsToggleRow(
                                icon = Icons.Default.Vibration,
                                label = "الاهتزاز التفاعلي",
                                checked = profile.vibrationEnabled,
                                onCheckedChange = onToggleVibration,
                                testTag = "toggle_vibration",
                                labelColor = textColor,
                                iconTint = brandAccent
                            )
                            HorizontalDivider(color = dividerColor)
                            SettingsToggleRow(
                                icon = Icons.Default.MusicNote,
                                label = "الموسيقى الهادئة",
                                checked = profile.musicEnabled,
                                onCheckedChange = onToggleMusic,
                                testTag = "toggle_music",
                                labelColor = textColor,
                                iconTint = brandAccent
                            )
                            HorizontalDivider(color = dividerColor)
                            SettingsToggleRow(
                                icon = Icons.Default.RecordVoiceOver,
                                label = "عبارات التشجيع",
                                checked = profile.voiceEnabled,
                                onCheckedChange = onToggleVoice,
                                testTag = "toggle_voice",
                                labelColor = textColor,
                                iconTint = brandAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // =========================================================================
                    // القسم 5: درع الأمان والخصوصية الحديدي (Zero Data Privacy & Security Shield)
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                            .testTag("security_shield_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "درع الأمان والخصوصية الحديدي",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    ),
                                    color = titleColor,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = if (isDark) 0.16f else 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الدرع نشط • حماية مطلقة 100% ضد سحب أي بيانات من هاتفك",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        ),
                                        color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Text(
                                text = "تطبيق Maze Arrows مصمم وفق أعلى معايير الأمان العالمية بمبدأ (Zero-Data Extraction): معالجة وحسابات محلية 100% على معالج جهازك بدون إرسال أي بايت لخوادم خارجية، وانعدام تام للصلاحيات الحساسة (الصور، الأسماء، الموقع، الميكروفون، الكاميرا)، وخالٍ نهائياً من أي أدوات تتبع أو إعلانات.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp
                                ),
                                color = subTextColor,
                                textAlign = TextAlign.Center
                            )

                            // Privacy Screen Capture Toggle (FLAG_SECURE)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = subCardBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "حظر لقطات الشاشة والتسجيل الخفي",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            ),
                                            color = textColor
                                        )
                                        Text(
                                            text = "حماية شاشة اللعبة من التطبيقات المتلصصة في الخلفية (FLAG_SECURE)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 10.5.sp
                                            ),
                                            color = subTextColor
                                        )
                                    }
                                    Switch(
                                        checked = screenPrivacyProtection,
                                        onCheckedChange = onToggleScreenPrivacyProtection,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF10B981)
                                        ),
                                        modifier = Modifier.testTag("toggle_screen_privacy")
                                    )
                                }
                            }

                            // Full Audit Details Button
                            Button(
                                onClick = onOpenSecurityShieldDialog,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFF059669),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_security_audit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تقرير فحص وتدقيق الأمان الشامل (Audit Report)",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // =========================================================================
                    // القسم 6: نظام التحديث والتنزيل بالميجات (MB) داخل التطبيق عبر GitHub
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                            .testTag("github_update_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تحديث وتنزيل إصدارات التطبيق",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    ),
                                    color = titleColor,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Side-by-side Version Comparison Pills (معرفة الإصدار القديم/الحالي والمقارنة دائماً)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = subCardBg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "الإصدار القديم/الحالي المثبّت",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                                            color = subTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "v${updateCheckState.installedVersionName}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp
                                            ),
                                            color = titleColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = subCardBg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (updateCheckState.isUpdateAvailable) Color(0xFF2E7D32) else cardBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "أحدث إصدار متاح",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                                            color = subTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = if (updateCheckState.remoteVersionName != null) {
                                                "v${updateCheckState.remoteVersionName}"
                                            } else {
                                                "لا يوجد أحدث"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            ),
                                            color = if (updateCheckState.isUpdateAvailable) Color(0xFF2E7D32) else Color(0xFF1565C0),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Comparison & In-App MB Status Banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (updateCheckState.isUpdateAvailable) {
                                    Color(0xFF2E7D32).copy(alpha = 0.16f)
                                } else {
                                    subCardBg
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = updateCheckState.comparisonSummaryAr,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (updateCheckState.isUpdateAvailable) Color(0xFF2E7D32) else titleColor,
                                        textAlign = TextAlign.Center
                                    )
                                    if (updateCheckState.isDownloading) {
                                        Text(
                                            text = "التنزيل من GitHub: ${GitHubAppUpdater.formatMb(updateCheckState.downloadedMegabytes)} من ${GitHubAppUpdater.formatMb(updateCheckState.totalMegabytes.coerceAtLeast(1f))} (${updateCheckState.downloadProgressPercent}%)",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            ),
                                            color = Color(0xFF1565C0),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            if (updateCheckState.isDownloading) {
                                LinearProgressIndicator(
                                    progress = { (updateCheckState.downloadProgressPercent / 100f).coerceIn(0f, 1f) },
                                    color = Color(0xFF1565C0),
                                    trackColor = cardBorder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            }

                            // Action Buttons: Compare Version + In-App Download (MB) & System Install
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCheckGitHubUpdate,
                                    enabled = !updateCheckState.isChecking && !updateCheckState.isDownloading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("check_github_version_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = brandAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "فحص الإصدار",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        color = titleColor
                                    )
                                }

                                Button(
                                    onClick = onDownloadGitHubUpdate,
                                    enabled = !updateCheckState.isDownloading && !updateCheckState.isChecking,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (updateCheckState.isUpdateAvailable || !updateCheckState.readyApkFilePath.isNullOrBlank()) {
                                            Color(0xFF2E7D32)
                                        } else {
                                            Color(0xFF1565C0)
                                        },
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1.35f)
                                        .height(46.dp)
                                        .testTag("download_github_update_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            updateCheckState.isDownloading ->
                                                "تنزيل ${updateCheckState.downloadProgressPercent}%"
                                            !updateCheckState.readyApkFilePath.isNullOrBlank() ->
                                                "تثبيت التحديث الآن"
                                            updateCheckState.isUpdateAvailable ->
                                                "تنزيل وتثبيت v${updateCheckState.remoteVersionName}"
                                            else ->
                                                "تحديث وتثبيت"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // =========================================================================
                    // القسم 7: بيانات المطور وعن التطبيق (منصف في المنتصف)
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                            .testTag("developer_info_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = brandAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "بيانات المطور وعن التطبيق",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    ),
                                    color = titleColor,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = subCardBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, brandAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "اسم المطور",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = subTextColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "محمد شهيد رحيم",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp
                                        ),
                                        color = titleColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Maze Arrows • الإصدار ${updateCheckState.installedVersionName} • آمن ومعتمد لجميع الهواتف",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = Color(0xFF2E7D32),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (showRestartButton) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onRestartLevel()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = brandAccent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("restart_level_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إعادة تشغيل المستوى الحالي",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    // Extra bottom spacing so content never touches or overlaps mobile navigation bars
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
fun AmazeEyeCareReminderDialog(
    themePalette: BoardThemePalette,
    eyeProtectionActive: Boolean,
    onEnableEyeProtection: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = themePalette.isDark
    val cardBg = if (isDark) Color(0xFF1C2433) else Color(0xFFFAF3E6)
    val titleColor = if (isDark) Color.White else Color(0xFF2B1B0E)
    val textColor = if (isDark) Color(0xFFE8DCCB) else Color(0xFF4E3117)
    val cardBorder = if (isDark) Color(0xFF384661) else Color(0xFFD7C6B0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, cardBorder, RoundedCornerShape(26.dp))
                .testTag("eye_care_reminder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2E7D32).copy(alpha = 0.18f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "حماية البصر",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "👀 تذكير راحة وحماية البصر",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = titleColor,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF242F42) else Color(0xFFF1E4CE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "لقد ركّزت في حل المتاهات لأكثر من 20 دقيقة متواصلة!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "لحماية عينيك وتقليل الإجهاد وفق قاعدة (20-20-20)، انظر إلى شيء بعيد لمدة 20 ثانية لتريح عضلات العينين.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (!eyeProtectionActive) {
                    Button(
                        onClick = onEnableEyeProtection,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("enable_eye_protection_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تفعيل وضع حماية البصر فوراً",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dismiss_eye_care_button")
                ) {
                    Text(
                        text = "أخذت استراحة قصيرة • متابعة اللعب",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = titleColor
                    )
                }
            }
        }
    }
}

@Composable
fun AmazePointsShopDialog(
    profile: PlayerProfileEntity,
    feedbackMessage: String?,
    isDark: Boolean = false,
    onPurchaseExtraTime: () -> Unit,
    onPurchaseUnlockTools: () -> Unit = {},
    onPurchaseStayInMode: (DifficultyTier) -> Unit,
    onUnlockAutoMode: () -> Unit,
    onDismiss: () -> Unit
) {
    val nowMs = System.currentTimeMillis()
    val recentPurchasesCount = GameRepository.parseRecentPurchaseTimestamps(profile.purchaseTimestampsCsv, nowMs).size
    val remainingPurchasesInHour = (GameRepository.MAX_PURCHASES_PER_HOUR - recentPurchasesCount).coerceAtLeast(0)
    val extraTimeCost = GameRepository.computeExtraTimeCost(profile.totalPurchasesCount)
    val unlockToolsCost = GameRepository.computeUnlockToolsCost(profile.totalPurchasesCount)
    val stayInModeCost = GameRepository.computeStayInModeCost(profile.totalPurchasesCount)
    val currentlyLockedTier = DifficultyTier.entries.firstOrNull { it.name == profile.lockedDifficultyTierName }
    var selectedModeToBuy by remember(currentlyLockedTier) {
        mutableStateOf(currentlyLockedTier ?: DifficultyTier.EASY)
    }

    val dialogBg = if (isDark) Color(0xFF181F2C) else Color(0xFFFAF3E6)
    val dialogBorder = if (isDark) Color(0xFF2C394F) else Color(0xFFE2D4BF)
    val titleColor = if (isDark) Color(0xFFFFB74D) else Color(0xFF2B1B0E)
    val textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF53351A)
    val bannerBg = if (isDark) Color(0xFF232D3F) else Color(0xFFEDE0CA)
    val bannerTitle = if (isDark) Color(0xFFFFCC80) else Color(0xFF8C4A15)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            border = BorderStroke(1.dp, dialogBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("points_shop_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "متجر النقاط التفاعلي",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = titleColor,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(36.dp)
                            .testTag("close_shop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Balance + Rate Limit Status Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bannerBg,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFDCC8AE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "رصيد النقاط الحالي: ${profile.totalScore} نقطة ⭐",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            ),
                            color = bannerTitle
                        )
                        Text(
                            text = "عمليات الشراء المتاحة في الساعة: $remainingPurchasesInHour / ${GameRepository.MAX_PURCHASES_PER_HOUR}  •  مستوى التصاعد: #${profile.totalPurchasesCount + 1}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (feedbackMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF8E7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFD9822B), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = feedbackMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF7A3E0A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Item 0: Unlock Hint, Path Inspect & Auto-Solve Cooldowns Immediately (+2 Hints)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF3E6D3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD7C6B0), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFD9822B),
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "فتح التلميح والمسار والحل التلقائي فوراً",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF2B1B0E)
                                )
                                Text(
                                    text = "إلغاء انتظار الـ 3 دقائق للتلميح وفحص المسار والحل التلقائي مع +2 تلميح إضافي",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = Color(0xFF53351A)
                                )
                            }
                        }

                        Button(
                            onClick = onPurchaseUnlockTools,
                            enabled = remainingPurchasesInHour > 0,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD9822B),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("buy_unlock_tools_button")
                        ) {
                            Text(
                                text = "فتح المساعدات فوراً مقابل $unlockToolsCost نقطة",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Item 1: Buy Extra Time (+60 Seconds)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF3E6D3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD7C6B0), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFFB36A22),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "شراء وقت إضافي (+60 ثانية)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color(0xFF2B1B0E)
                                    )
                                    Text(
                                        text = "يُضاف فوراً للمؤقت وتزداد تكلفته تدريجياً مع كل شراء",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = Color(0xFF53351A)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onPurchaseExtraTime,
                            enabled = remainingPurchasesInHour > 0,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB36A22),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("buy_extra_time_button")
                        ) {
                            Text(
                                text = "شراء مقابل $extraTimeCost نقطة",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Item 2: Stay in Specific Mode (ميزة البقاء في وضع معين)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF3E6D3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD7C6B0), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF7B1FA2),
                                modifier = Modifier.size(21.dp)
                            )
                            Column {
                                Text(
                                    text = "ميزة البقاء في وضع معين",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = Color(0xFF2B1B0E)
                                )
                                Text(
                                    text = if (currentlyLockedTier != null) {
                                        "الوضع المثبّت حالياً: ${currentlyLockedTier.arabicLabel}"
                                    } else {
                                        "ثبّت صعوبة اللعب في وضعك المفضل بدون تصاعد إجباري"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = Color(0xFF53351A)
                                )
                            }
                        }

                        // Mode selector row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (tier in DifficultyTier.entries) {
                                val isSelected = tier == selectedModeToBuy
                                Surface(
                                    onClick = { selectedModeToBuy = tier },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(tier.badgeColorHex) else Color(0xFFFAF3E6),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = tier.arabicLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        color = if (isSelected) Color.White else Color(0xFF4E3117),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onPurchaseStayInMode(selectedModeToBuy) },
                            enabled = remainingPurchasesInHour > 0,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7B1FA2),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("buy_stay_in_mode_button")
                        ) {
                            Text(
                                text = "تثبيت وضع (${selectedModeToBuy.arabicLabel}) مقابل $stayInModeCost نقطة",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (currentlyLockedTier != null) {
                            OutlinedButton(
                                onClick = onUnlockAutoMode,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Text(
                                    text = "إلغاء تثبيت الوضع والعودة للتنويع التلقائي",
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                    color = Color(0xFF53351A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    labelColor: Color = Color(0xFF4E3117),
    iconTint: Color = Color(0xFFB36A22)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = labelColor
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconTint,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (labelColor == Color(0xFF4E3117)) Color(0xFFD7C6B0) else Color(0xFF475569)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun AmazeThemeDialog(
    selectedThemeId: String,
    isDark: Boolean = false,
    onSelectTheme: (BoardThemePalette) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF181F2C) else Color(0xFFFAF3E6)
    val dialogBorder = if (isDark) Color(0xFF2C394F) else Color(0xFFE2D4BF)
    val titleColor = if (isDark) Color(0xFFFFB74D) else Color(0xFF4E3117)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B4624)
    val optionBg = if (isDark) Color(0xFF232D3F) else Color(0xFFFFFBF4)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            border = BorderStroke(1.dp, dialogBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ألوان الأسهم والشبكة",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = titleColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "خصص نسق ألوان الأسهم ونقاط الشبكة على رقعة اللعب بحرية دون التأثير على ثيم التطبيق المختار في الإعدادات:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = subTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                for (palette in BoardThemePalette.entries) {
                    val isSelected = palette.id == selectedThemeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(optionBg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFD9822B) else if (isDark) Color(0xFF334155) else Color(0xFFD7C6B0),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onSelectTheme(palette)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(palette.primaryArrowHex))
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(palette.highlightArrowHex))
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(palette.dotColorHex))
                            )
                        }

                        Text(
                            text = palette.arabicName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF2B1B0E),
                            modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "محدد",
                                tint = Color(0xFFD9822B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmazeVictoryDialog(
    levelNumber: Int,
    isCustom: Boolean,
    usedAutoSolve: Boolean,
    starsEarned: Int,
    scoreEarned: Int,
    elapsedSeconds: Int,
    isDark: Boolean = false,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF181F2C) else Color(0xFFFAF3E6)
    val dialogBorder = if (isDark) Color(0xFF2C394F) else Color(0xFFE2D4BF)
    val titleColor = if (isDark) Color(0xFFFFB74D) else Color(0xFF4E3117)
    val textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF53351A)
    val pillBg = if (isDark) Color(0xFF232D3F) else Color(0xFFEDE0CA)
    val pillTextColor = if (isDark) Color(0xFFFFCC80) else Color(0xFF6B421B)
    val replayBtnColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF53351A)

    var triggerCelebration by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        triggerCelebration = true
    }

    val burstProgress by animateFloatAsState(
        targetValue = if (triggerCelebration) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "victoryBurstProgress"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "victorySparkleLoop")
    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinklePhase"
    )
    val starPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starPulse"
    )

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            border = BorderStroke(1.dp, dialogBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("victory_dialog")
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (!usedAutoSolve) {
                    VictoryCelebrationStarsCanvas(
                        burstProgress = burstProgress,
                        twinklePhase = twinklePhase,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (usedAutoSolve) {
                        Text(
                            text = "انتهى عرض الحل التلقائي",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                            color = Color(0xFF9C27B0),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "تم عرض طريقة الحل للتعلّم فقط. لا يمكن تجاوز المستوى أو فتح المستوى التالي إلا بعد حلّه بنفسك!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = textColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onReplay,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB36A22),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("replay_manual_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "أعد المستوى وحلّه بنفسك",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        Text(
                            text = if (isCustom) "اكتمل المستوى المخصص! 🌟" else "اكتمل المستوى $levelNumber بنجاح! 🌟",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (s in 1..3) {
                                val isEarned = s <= starsEarned
                                val starPopScale by animateFloatAsState(
                                    targetValue = if (triggerCelebration && isEarned) 1f else if (triggerCelebration) 0.85f else 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "starPop_$s"
                                )
                                val combinedScale = if (isEarned) starPopScale * starPulse else starPopScale
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isEarned) Color(0xFFF3A833) else if (isDark) Color(0xFF475569) else Color(0xFFD7C6B0),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .scale(combinedScale)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = pillBg,
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFDCC8AE))
                        ) {
                            Text(
                                text = "النقاط المكتسبة: +$scoreEarned ⭐  •  الوقت: ${elapsedSeconds}ث",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = pillTextColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onNextLevel,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB36A22),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("next_level_button")
                        ) {
                            Text(
                                text = if (isCustom) "مستوى ذكي تالٍ" else "المستوى التالي (المستوى ${levelNumber + 1})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onReplay,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFD7C6B0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "إعادة اللعب",
                                color = replayBtnColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VictoryCelebrationStarsCanvas(
    burstProgress: Float,
    twinklePhase: Float,
    modifier: Modifier = Modifier
) {
    val starColors = remember {
        listOf(
            Color(0xFFFFB300),
            Color(0xFFFF8F00),
            Color(0xFF43A047),
            Color(0xFF1E88E5),
            Color(0xFF8E24AA),
            Color(0xFFFDD835)
        )
    }
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.45f
        val maxRadius = size.width * 0.44f
        val particleCount = 16

        for (i in 0 until particleCount) {
            val baseAngle = (i.toFloat() / particleCount) * (2f * PI.toFloat())
            val ringFactor = if (i % 2 == 0) 1.0f else 0.62f
            val distance = maxRadius * ringFactor * burstProgress
            val wobble = sin(twinklePhase + i * 0.7f) * 6f
            val px = cx + cos(baseAngle) * distance + wobble
            val py = cy + sin(baseAngle) * (distance * 0.68f) - (1f - burstProgress) * 14f

            val twinkleScale = 0.65f + 0.35f * ((sin(twinklePhase * 1.5f + i) + 1f) * 0.5f)
            val starRadius = (if (i % 3 == 0) 11f else 8f) * twinkleScale * burstProgress
            val color = starColors[i % starColors.size].copy(alpha = (0.85f * burstProgress).coerceIn(0f, 1f))

            // Draw 4-point sparkle star
            val path = Path().apply {
                moveTo(px, py - starRadius)
                lineTo(px + starRadius * 0.32f, py - starRadius * 0.32f)
                lineTo(px + starRadius, py)
                lineTo(px + starRadius * 0.32f, py + starRadius * 0.32f)
                lineTo(px, py + starRadius)
                lineTo(px - starRadius * 0.32f, py + starRadius * 0.32f)
                lineTo(px - starRadius, py)
                lineTo(px - starRadius * 0.32f, py - starRadius * 0.32f)
                close()
            }
            drawPath(path = path, color = color)
        }
    }
}

@Composable
fun AmazeGameOverDialog(
    levelNumber: Int = 1,
    currentDifficulty: DifficultyTier = DifficultyTier.EASY,
    profile: PlayerProfileEntity = PlayerProfileEntity(),
    feedbackMessage: String? = null,
    isTimeOut: Boolean = false,
    isDark: Boolean = false,
    onBuyAttemptsAndContinue: () -> Unit = {},
    onBuyStayInSameLevel: () -> Unit = {},
    onRefillLives: () -> Unit,
    onRestartLevel: () -> Unit
) {
    val extraAttemptsCost = GameRepository.computeExtraAttemptsCost(profile.totalPurchasesCount)
    val stayInSameLevelCost = GameRepository.computeStayInModeCost(profile.totalPurchasesCount)

    val dialogBg = if (isDark) Color(0xFF181F2C) else Color(0xFFFAF3E6)
    val dialogBorder = if (isDark) Color(0xFF2C394F) else Color(0xFFE2D4BF)
    val titleColor = if (isDark) Color(0xFFFF6B6B) else Color(0xFFC62828)
    val textColor = if (isDark) Color(0xFFE2DCD2) else Color(0xFF53351A)
    val pillBg = if (isDark) Color(0xFF232D3F) else Color(0xFFEDE0CA)
    val pillTextColor = if (isDark) Color(0xFFFFB74D) else Color(0xFF8C4A15)
    val dividerColor = if (isDark) Color(0xFF2C394F) else Color(0xFFD7C6B0)
    val freeButtonBorder = if (isDark) Color(0xFFD9822B) else Color(0xFFB36A22)
    val freeButtonText = if (isDark) Color(0xFFFFB74D) else Color(0xFF8C4A15)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            border = BorderStroke(1.dp, dialogBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTimeOut) "انتهى الوقت! ⏱" else "نفدت المحاولات! 💧",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = titleColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "تابع تقدمك بالنقاط أو ابدأ محاولة جديدة:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current Points Balance Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = pillBg,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFDCC8AE))
                ) {
                    Text(
                        text = "رصيدك الحالي: ${profile.totalScore} نقطة ⭐",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp
                        ),
                        color = pillTextColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                if (feedbackMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF2A241C) else Color(0xFFFFF8E7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFD9822B), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = feedbackMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = if (isDark) Color(0xFFFFCC80) else Color(0xFF7A3E0A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: Buy +5 Attempts (Warm Amber/Bronze - High Contrast & Harmonious)
                Button(
                    onClick = onBuyAttemptsAndContinue,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD9822B),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("buy_attempts_continue_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "متابعة اللعب (+5 محاولات) • $extraAttemptsCost ⭐",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Buy Stay in the Same Level & Mode (Refined Slate / Warm Navy)
                Button(
                    onClick = onBuyStayInSameLevel,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF2C394F) else Color(0xFF5D4037),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("buy_stay_same_level_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تثبيت وضع (${currentDifficulty.arabicLabel}) • $stayInSameLevelCost ⭐",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = dividerColor)
                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: Free fallback — clean friendly retry
                OutlinedButton(
                    onClick = onRefillLives,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, if (isDark) Color(0xFF475569) else Color(0xFFC7B59F)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF53351A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restart_from_scratch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFE2E8F0) else Color(0xFF53351A),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "محاولة جديدة مجاناً (5 محاولات)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AmazeSecurityShieldDialog(
    report: SecurityAuditReport?,
    isDark: Boolean,
    onRefreshAudit: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF131822) else Color(0xFFFAF3E6)
    val cardBg = if (isDark) Color(0xFF1E2638) else Color(0xFFFFFDF9)
    val subCardBg = if (isDark) Color(0xFF161E2D) else Color(0xFFF6EDE0)
    val cardBorder = if (isDark) Color(0xFF2E3D59) else Color(0xFFE0D0BB)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF2B1B0E)
    val textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF4E3117)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF785434)
    val emeraldGreen = Color(0xFF10B981)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            color = dialogBg,
            modifier = Modifier
                .fillMaxSize()
                .testTag("security_shield_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Surface(
                    color = if (isDark) Color(0xFF1C2433) else Color(0xFFEDE0CA),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = emeraldGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "درع الأمان والخصوصية الحديدي",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    ),
                                    color = titleColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = "حماية غير قابلة للاختراق • منع سحب أي بيانات من هاتفك",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = emeraldGreen,
                                textAlign = TextAlign.Center
                            )
                        }
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF2B364C) else Color(0xFFFAF3E6),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(38.dp)
                                .testTag("close_security_dialog_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = if (isDark) Color(0xFFE8DCCB) else Color(0xFF53351A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status Badge Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = emeraldGreen.copy(alpha = if (isDark) 0.18f else 0.12f),
                        border = BorderStroke(1.5.dp, emeraldGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = emeraldGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = report?.overallStatusAr ?: "الدرع الحديدي نشط • محمي 100%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = "تم فحص بيئة التشغيل وأذونات النظام: التطبيق معزول تماماً وممنوع بموجب قواعد حماية أندرويد الصارمة من سحب أي صور، أو أسماء، أو موقع، أو ملفات، أو أرقام، أو تسجيلات صوتية.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                ),
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Security Pillars Breakdown
                    report?.items?.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (item.isSecure) emeraldGreen.copy(alpha = 0.18f) else Color(0xFFEF4444).copy(alpha = 0.18f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (item.isSecure) Icons.Default.Check else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (item.isSecure) emeraldGreen else Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.titleAr,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = textColor
                                    )
                                    Text(
                                        text = item.descriptionAr,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.5.sp,
                                            lineHeight = 14.5.sp
                                        ),
                                        color = subTextColor
                                    )
                                    Text(
                                        text = "الحالة التقنية: ${item.technicalDetail}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (item.isSecure) emeraldGreen else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Buttons
                    Button(
                        onClick = onRefreshAudit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = emeraldGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recheck_security_audit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إعادة فحص سلامة النظام وتدقيق الأمان الآن",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "إغلاق نافذة الأمان",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

