package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LevelProgressEntity
import com.example.data.PlayerProfileEntity
import com.example.engine.ArrowTipStyle
import com.example.engine.BoardShapeMask
import com.example.engine.BoardThemePalette
import com.example.engine.DifficultyTier
import com.example.engine.LevelAlgorithmArchetype
import com.example.engine.SmartLevelGenerator
import com.example.ui.components.AmazeBoardCanvas
import com.example.ui.components.AmazeFloatingToastBanner
import com.example.ui.components.AmazeGameOverDialog
import com.example.ui.components.AmazeGameplayBottomBar
import com.example.ui.components.AmazeGameplaySubBar
import com.example.ui.components.AmazeGameplayTopBar
import com.example.ui.components.AmazeVictoryDialog
import com.example.viewmodel.GameplayUiState

import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Shield

@Composable
fun MainMenuScreen(
    profile: PlayerProfileEntity,
    themePalette: BoardThemePalette,
    onContinueLevel: (Int) -> Unit,
    onRandomizePlayerDna: () -> Unit = {},
    onOpenLevelSelect: () -> Unit,
    onOpenCustomGenerator: () -> Unit,
    onOpenShop: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenSecurityShield: () -> Unit = {}
) {
    val bgColor = Color(themePalette.boardBackgroundHex)
    val cardColor = Color(themePalette.cardSurfaceHex)
    val primaryText = if (themePalette.isDark) Color(0xFFFFFFFF) else Color(0xFF2B1B0E)
    val secondaryText = if (themePalette.isDark) Color(0xFFE2E8F0) else Color(0xFF4E3117)
    val cardBorderColor = if (themePalette.isDark) Color(0xFF334155) else Color(0xFFE2D3BE)
    val accentGold = Color(themePalette.highlightArrowHex)
    val lockedTier = DifficultyTier.entries.firstOrNull { it.name == profile.lockedDifficultyTierName }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Pinned App Name at Top-Center with ultra-clear high-contrast typography in both Light & Dark modes
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardColor,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Maze Arrows",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 27.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        maxLines = 1,
                        color = primaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ألغاز المسارات المتشابكة الذكية",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (themePalette.isDark) Color(0xFF2D2212) else Color(0xFFFFEBC8),
                        modifier = Modifier
                            .border(
                                width = 1.5.dp,
                                color = if (themePalette.isDark) Color(0xFFFFB74D) else Color(0xFFD9822B),
                                shape = RoundedCornerShape(50)
                            )
                            .testTag("player_current_level_badge")
                    ) {
                        Text(
                            text = "مستوى اللاعب: ${profile.highestUnlockedLevel}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            ),
                            color = if (themePalette.isDark) Color(0xFFFFE082) else Color(0xFF4A2506),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Spaced Action Bar below the App Title (إزاحة أيقونة الإعدادات لأسفل وإلى الجانب بمسافة مريحة)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Points & Stars Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenShop,
                        shape = RoundedCornerShape(50),
                        color = cardColor,
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp,
                        modifier = Modifier.testTag("menu_points_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "متجر النقاط",
                                tint = Color(0xFFD9822B),
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "${profile.totalScore} نقطة",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = primaryText
                            )
                        }
                    }

                    StatBadgeChip(
                        icon = Icons.Default.Star,
                        iconTint = Color(0xFFF3A833),
                        text = "${profile.totalStars}",
                        cardColor = cardColor,
                        textColor = primaryText
                    )
                }

                // Side Icons (Theme & Settings shifted down and to the side with clear spacing)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenThemes,
                        shape = CircleShape,
                        color = cardColor,
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("menu_theme_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "تغيير المظهر",
                                tint = primaryText,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenSecurityShield,
                        shape = CircleShape,
                        color = cardColor,
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("menu_security_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "درع الأمان والخصوصية",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        color = cardColor,
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("menu_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = primaryText,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Security & Privacy Shield Guarantee Banner
            Surface(
                onClick = onOpenSecurityShield,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = if (themePalette.isDark) 0.16f else 0.10f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("menu_security_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🛡️ درع أمان وحديدي: منع سحب أي بيانات من الهاتف بنسبة 100%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = if (themePalette.isDark) Color(0xFF34D399) else Color(0xFF047857),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Hero Decorative Spiral Arrow Illustration Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeroSpiralArrowsPreview(
                        primaryColor = primaryText,
                        accentColor = accentGold,
                        dotColor = Color(themePalette.dotColorHex),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "فك تشابك الأسهم بالترتيب الصحيح قبل انتهاء الوقت",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Primary Play CTA
            val playModeLabel = lockedTier?.arabicLabel
                ?: DifficultyTier.progressiveSubtitle(profile.highestUnlockedLevel)
            Button(
                onClick = { onContinueLevel(profile.highestUnlockedLevel) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB36A22),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("play_active_level_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "العب المستوى ${profile.highestUnlockedLevel}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = playModeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Interactive Points Shop Card (متجر النقاط التفاعلي)
            Card(
                onClick = onOpenShop,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                    .testTag("open_points_shop_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "متجر النقاط التفاعلي (${profile.totalScore} نقطة)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = primaryText
                        )
                        Text(
                            text = "فتح التلميح والمسار والحل التلقائي فوراً أو وقت إضافي",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = secondaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFD9822B).copy(alpha = 0.22f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = if (themePalette.isDark) Color(0xFFFFB74D) else Color(0xFFB36A22)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Previous Passed Levels Quick-Return Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (themePalette.isDark) Color(0xFFFFB74D) else Color(0xFFB36A22),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "المستويات المفتوحة (المستوى 1 إلى المستوى ${profile.highestUnlockedLevel})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = primaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val unlockedList = remember(profile.highestUnlockedLevel) {
                        (1..profile.highestUnlockedLevel).toList()
                    }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(unlockedList, key = { it }) { lvl ->
                            OutlinedButton(
                                onClick = { onContinueLevel(lvl) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                                modifier = Modifier.testTag("quick_unlocked_level_$lvl")
                            ) {
                                Text(
                                    text = if (lvl < profile.highestUnlockedLevel) "المستوى $lvl ✓" else "المستوى $lvl ▶",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    color = primaryText
                                )
                            }
                        }

                        item(key = "locked_next") {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (themePalette.isDark) Color(0xFF283246) else Color(0xFFEDE0CA),
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = cardBorderColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "مقفل",
                                        tint = secondaryText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "المستوى ${profile.highestUnlockedLevel + 1}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        color = secondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Level Select (500 Progressive Levels across 5 Tiers)
            Card(
                onClick = onOpenLevelSelect,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                    .testTag("open_level_select_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "خريطة المراحل (500 مرحلة)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = primaryText
                        )
                        Text(
                            text = "تدرج تلقائي في الصعوبة مع حفظ التقدم",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = secondaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF3A833).copy(alpha = 0.22f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = if (themePalette.isDark) Color(0xFFFFCA28) else Color(0xFFB36A22)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smart Generator & Solver Studio (Clean Professional Title)
            Card(
                onClick = onOpenCustomGenerator,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                    .testTag("open_custom_generator_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مختبر التوليد الذكي",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = primaryText
                        )
                        Text(
                            text = "توليد مراحل مخصصة مع فحص الحل",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = secondaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF7B1FA2).copy(alpha = if (themePalette.isDark) 0.28f else 0.16f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (themePalette.isDark) Color(0xFFCE93D8) else Color(0xFF7B1FA2)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatArabicLevelTitle(level: Int): String {
    val ordinal = when (level) {
        1 -> "المستوى الأول"
        2 -> "المستوى الثاني"
        3 -> "المستوى الثالث"
        4 -> "المستوى الرابع"
        5 -> "المستوى الخامس"
        6 -> "المستوى السادس"
        7 -> "المستوى السابع"
        8 -> "المستوى الثامن"
        9 -> "المستوى التاسع"
        10 -> "المستوى العاشر"
        else -> null
    }
    return if (ordinal != null) "المستوى $level ($ordinal)" else "المستوى $level"
}

@Composable
private fun StatBadgeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String,
    cardColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = cardColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
        }
    }
}

@Composable
private fun HeroSpiralArrowsPreview(
    primaryColor: Color,
    accentColor: Color,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cols = 13
        val rows = 6
        val step = minOf(size.width / (cols + 1), size.height / (rows + 1))
        val startX = (size.width - (cols - 1) * step) / 2f
        val startY = (size.height - (rows - 1) * step) / 2f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                drawCircle(
                    color = dotColor,
                    radius = 2.5f,
                    center = Offset(startX + c * step, startY + r * step)
                )
            }
        }

        val strokeW = (step * 0.29f).coerceAtLeast(4.5f)

        val spiral1 = listOf(
            3 to 3, 4 to 3, 4 to 2, 2 to 2, 2 to 4, 5 to 4, 5 to 1, 1 to 1, 1 to 5
        )
        val path1 = Path().apply {
            moveTo(startX + spiral1[0].first * step, startY + spiral1[0].second * step)
            for (i in 1 until spiral1.size) {
                lineTo(startX + spiral1[i].first * step, startY + spiral1[i].second * step)
            }
        }
        drawPath(
            path = path1,
            color = primaryColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val h1x = startX + 1 * step
        val h1y = startY + 5 * step
        drawPath(
            path = Path().apply {
                moveTo(h1x, h1y + step * 0.30f)
                lineTo(h1x - step * 0.28f, h1y - step * 0.16f)
                lineTo(h1x + step * 0.28f, h1y - step * 0.16f)
                close()
            },
            color = primaryColor
        )

        val spiral2 = listOf(
            8 to 2, 9 to 2, 9 to 3, 7 to 3, 7 to 1, 11 to 1, 11 to 4, 7 to 4
        )
        val path2 = Path().apply {
            moveTo(startX + spiral2[0].first * step, startY + spiral2[0].second * step)
            for (i in 1 until spiral2.size) {
                lineTo(startX + spiral2[i].first * step, startY + spiral2[i].second * step)
            }
        }
        drawPath(
            path = path2,
            color = accentColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val h2x = startX + 7 * step
        val h2y = startY + 4 * step
        drawPath(
            path = Path().apply {
                moveTo(h2x - step * 0.30f, h2y)
                lineTo(h2x + step * 0.16f, h2y - step * 0.28f)
                lineTo(h2x + step * 0.16f, h2y + step * 0.28f)
                close()
            },
            color = accentColor
        )
    }
}

@Composable
fun LevelSelectScreen(
    selectedTier: DifficultyTier,
    levelProgressMap: Map<Int, LevelProgressEntity>,
    highestUnlockedLevel: Int,
    themePalette: BoardThemePalette,
    onSelectTier: (DifficultyTier) -> Unit,
    onStartLevel: (Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val bgColor = Color(themePalette.boardBackgroundHex)
    val cardColor = Color(themePalette.cardSurfaceHex)
    val primaryText = if (themePalette.isDark) Color(0xFFFFFFFF) else Color(0xFF2B1B0E)
    val secondaryText = if (themePalette.isDark) Color(0xFFE2E8F0) else Color(0xFF4E3117)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("level_select_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = primaryText
                )
            }
            Text(
                text = "خريطة المستويات (المفتوح: 1 إلى $highestUnlockedLevel)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = primaryText,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // 5 Difficulty Tier Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (tier in DifficultyTier.entries) {
                val isSelected = tier == selectedTier
                val tierColor = Color(tier.badgeColorHex)
                Surface(
                    onClick = { onSelectTier(tier) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) tierColor else cardColor,
                    modifier = Modifier.testTag("tier_tab_${tier.name.lowercase()}")
                ) {
                    Text(
                        text = "${tier.arabicLabel} (${tier.levelRange.first}-${tier.levelRange.last})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else primaryText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Tier Info Card (Strict progression explanation)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "فئة ${selectedTier.arabicLabel} (${selectedTier.englishLabel})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (themePalette.isDark) Color(0xFFFFCC80) else Color(selectedTier.badgeColorHex)
                )
                Text(
                    text = "${selectedTier.descriptionAr} • يمكنك دائماً الرجوع لأي مستوى عبرته (1–$highestUnlockedLevel)، بينما تفتح المستويات الأعلى بالترتيب عند الفوز.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = secondaryText
                )
            }
        }

        val levelsInTier = remember(selectedTier) { selectedTier.levelRange.toList() }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 78.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(levelsInTier, key = { it }) { lvlNum ->
                val progress = levelProgressMap[lvlNum]
                // Strict rule: ONLY unlocked if lvlNum <= highestUnlockedLevel!
                val isUnlocked = lvlNum <= highestUnlockedLevel
                val isCompleted = (progress?.isCompleted == true) || (lvlNum < highestUnlockedLevel)
                val stars = progress?.starsEarned ?: if (lvlNum < highestUnlockedLevel) 3 else 0

                Card(
                    onClick = {
                        if (isUnlocked) {
                            onStartLevel(lvlNum)
                        }
                    },
                    enabled = isUnlocked,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCompleted -> if (themePalette.isDark) Color(0xFF233228) else Color(0xFFF3E6D0)
                            isUnlocked -> cardColor
                            else -> cardColor.copy(alpha = 0.65f)
                        },
                        disabledContainerColor = cardColor.copy(alpha = 0.60f)
                    ),
                    modifier = Modifier
                        .height(84.dp)
                        .border(
                            width = if (lvlNum == highestUnlockedLevel) 2.dp else 1.dp,
                            color = if (lvlNum == highestUnlockedLevel) {
                                Color(0xFFF3A833)
                            } else {
                                primaryText.copy(alpha = 0.22f)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("level_card_$lvlNum")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "المستوى $lvlNum",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            color = if (isUnlocked) primaryText else secondaryText.copy(alpha = 0.65f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (isUnlocked) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (s in 1..3) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (s <= stars) Color(0xFFF3A833) else primaryText.copy(alpha = 0.28f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "مقفل",
                                tint = secondaryText.copy(alpha = 0.65f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomGeneratorScreen(
    uiState: GameplayUiState,
    themePalette: BoardThemePalette,
    onUpdateOptions: (DifficultyTier, BoardShapeMask, LevelAlgorithmArchetype) -> Unit,
    onRegenerate: () -> Unit,
    onPlayPreview: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val bgColor = Color(themePalette.boardBackgroundHex)
    val cardColor = Color(themePalette.cardSurfaceHex)
    val primaryText = Color(themePalette.primaryArrowHex)
    val previewLevel = uiState.previewCustomLevel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("generator_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = primaryText
                )
            }
            Text(
                text = "مولّد الحركات العكسية + فاحص Solver",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = primaryText,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "1. مستوى الصعوبة والكثافة:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = primaryText,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (tier in DifficultyTier.entries) {
                val isSelected = tier == uiState.customDifficulty
                Surface(
                    onClick = { onUpdateOptions(tier, uiState.customShapeMask, uiState.customArchetype) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) Color(tier.badgeColorHex) else cardColor,
                    modifier = Modifier.testTag("gen_diff_${tier.name.lowercase()}")
                ) {
                    Text(
                        text = tier.arabicLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else primaryText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "2. نمط خوارزمية البناء العكسي:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = primaryText,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (arch in LevelAlgorithmArchetype.entries) {
                val isSelected = arch == uiState.customArchetype
                Surface(
                    onClick = { onUpdateOptions(uiState.customDifficulty, uiState.customShapeMask, arch) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) Color(0xFF7B1FA2) else cardColor,
                    modifier = Modifier.testTag("gen_arch_${arch.name.lowercase()}")
                ) {
                    Text(
                        text = arch.arabicTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else primaryText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "3. هندسة اللوحة:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = primaryText,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (mask in BoardShapeMask.entries) {
                val isSelected = mask == uiState.customShapeMask
                Surface(
                    onClick = { onUpdateOptions(uiState.customDifficulty, mask, uiState.customArchetype) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) Color(0xFFB36A22) else cardColor,
                    modifier = Modifier.testTag("gen_mask_${mask.name.lowercase()}")
                ) {
                    Text(
                        text = mask.arabicName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else primaryText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isGeneratingLevel || previewLevel == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFB36A22))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                    ) {
                        AmazeBoardCanvas(
                            level = previewLevel,
                            remainingArrows = previewLevel.arrows,
                            escapingArrows = emptyList(),
                            blockedFeedback = null,
                            hintedArrowId = null,
                            themePalette = themePalette,
                            zoomResetTrigger = 0,
                            onArrowTapped = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val metrics = previewLevel.solverMetrics
                    val verifiedGreen = if (themePalette.isDark) Color(0xFF81C784) else Color(0xFF1B5E20)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2E7D32).copy(alpha = if (themePalette.isDark) 0.22f else 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = verifiedGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "تم التحقق بالـ Solver: قابلة للحل 100% • امتلاء ${previewLevel.fillDensityPercent}% بدون فجوات",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = verifiedGreen
                                )
                            }
                            Text(
                                text = "الخوارزمية: ${previewLevel.algorithmArchetype.arabicTitle}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = primaryText
                            )
                            Text(
                                text = "الشبكة: ${previewLevel.gridWidth}×${previewLevel.gridHeight}  •  الأسهم: ${metrics.totalArrows}  •  الحرة بالبداية: ${metrics.initialMovableCount}  •  موجات الحل: ${metrics.dependencyWaves}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = primaryText
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onRegenerate,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("regenerate_custom_button")
            ) {
                Icon(imageVector = Icons.Default.Casino, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "توليد جديد",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = primaryText
                )
            }

            Button(
                onClick = onPlayPreview,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB36A22),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("start_custom_level_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ابدأ اللعب",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun GameplayScreen(
    uiState: GameplayUiState,
    profile: PlayerProfileEntity,
    themePalette: BoardThemePalette,
    onArrowTapped: (Int) -> Unit,
    onPreviousLevelClick: () -> Unit,
    onNextUnlockedLevelClick: () -> Unit,
    onHintClick: () -> Unit,
    onUndoClick: () -> Unit,
    onTogglePreviewMode: () -> Unit,
    onAutoSolveToggle: () -> Unit,
    onResetZoomClick: () -> Unit,
    onRestartLevel: () -> Unit,
    onNextLevel: () -> Unit,
    onRefillLives: () -> Unit,
    onBuyAttemptsAndContinue: () -> Unit = {},
    onBuyStayInSameLevel: () -> Unit = {},
    onOpenShop: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenThemes: () -> Unit,
    onBackToMenu: () -> Unit,
    onArrowLongPressed: (Int) -> Unit = {},
    onArrowHoldReleased: (Int) -> Unit = {},
    onClearPreview: () -> Unit = {}
) {
    BackHandler(onBack = onBackToMenu)

    val level = uiState.activeLevel ?: return
    val bgColor = Color(themePalette.boardBackgroundHex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // 1. Top Header Bar
        AmazeGameplayTopBar(
            levelNumber = level.levelNumber,
            highestUnlockedLevel = profile.highestUnlockedLevel,
            isCustomLevel = level.isCustomGenerated,
            difficulty = level.difficulty,
            themePalette = themePalette,
            totalScore = profile.totalScore,
            isModeLocked = profile.lockedDifficultyTierName.isNotBlank(),
            onBackClick = onBackToMenu,
            onPreviousLevelClick = onPreviousLevelClick,
            onNextUnlockedLevelClick = onNextUnlockedLevelClick,
            onShopClick = onOpenShop,
            onThemeClick = onOpenThemes,
            onSettingsClick = onOpenSettings
        )

        // 2. Sub-bar with 3 Droplets/Lives, Live Countdown Timer, Remaining Arrows, Combo, and Hint button
        AmazeGameplaySubBar(
            livesRemaining = uiState.livesRemaining,
            maxLives = uiState.maxLives,
            remainingSeconds = uiState.remainingSeconds,
            remainingArrowsCount = uiState.remainingArrows.size,
            totalArrowsCount = level.arrows.size,
            comboCount = uiState.comboCount,
            hintsBalance = profile.hintsBalance,
            themePalette = themePalette,
            onHintClick = onHintClick
        )

        // 3. Main Interactive Board Area with Dotted Black Path Preview & Floating Toast
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            AmazeBoardCanvas(
                level = level,
                remainingArrows = uiState.remainingArrows,
                escapingArrows = uiState.escapingArrows,
                blockedFeedback = uiState.blockedFeedback,
                hintedArrowId = uiState.hintedArrowId,
                themePalette = themePalette,
                zoomResetTrigger = uiState.zoomResetTrigger,
                onArrowTapped = onArrowTapped,
                modifier = Modifier.fillMaxSize(),
                selectedPreviewArrowId = uiState.selectedPreviewArrowId,
                arrowTipStyle = ArrowTipStyle.fromId(profile.arrowTipStyle),
                eyeProtectionMode = profile.eyeProtectionMode,
                onArrowLongPressed = onArrowLongPressed,
                onArrowHoldReleased = onArrowHoldReleased,
                onClearPreview = onClearPreview
            )

            AmazeFloatingToastBanner(
                message = uiState.topToastBanner,
                modifier = Modifier.padding(top = 6.dp)
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.encouragementBanner != null && uiState.topToastBanner == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFDF9A44),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = uiState.encouragementBanner ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 4. Bottom Action Dock
        AmazeGameplayBottomBar(
            canUndo = uiState.undoStack.isNotEmpty(),
            previewModeEnabled = uiState.previewModeEnabled,
            isAutoSolving = uiState.isAutoSolving,
            themePalette = themePalette,
            onUndoClick = onUndoClick,
            onHintClick = onHintClick,
            onTogglePreviewMode = onTogglePreviewMode,
            onAutoSolveToggle = onAutoSolveToggle,
            onResetZoomClick = onResetZoomClick,
            pathInspectCooldownSeconds = uiState.pathInspectCooldownSeconds,
            hintCooldownSeconds = uiState.hintCooldownSeconds,
            autoSolveCooldownSeconds = uiState.autoSolveCooldownSeconds,
            freeHintsRemaining = uiState.freeHintsRemaining,
            purchasedHintsRemaining = uiState.purchasedHintsRemaining,
            freePathInspectsRemaining = uiState.freePathInspectsRemaining,
            purchasedPathInspectsRemaining = uiState.purchasedPathInspectsRemaining
        )
    }

    if (uiState.isVictory) {
        AmazeVictoryDialog(
            levelNumber = level.levelNumber,
            isCustom = level.isCustomGenerated,
            usedAutoSolve = uiState.usedAutoSolveInCurrentLevel,
            starsEarned = uiState.earnedStarsOnVictory,
            scoreEarned = uiState.currentLevelScore,
            elapsedSeconds = uiState.elapsedSeconds,
            isDark = themePalette.isDark,
            onNextLevel = onNextLevel,
            onReplay = onRestartLevel
        )
    }

    if (uiState.isGameOver) {
        AmazeGameOverDialog(
            isTimeOut = uiState.isTimeOutGameOver,
            levelNumber = level.levelNumber,
            currentDifficulty = level.difficulty,
            profile = profile,
            feedbackMessage = uiState.shopFeedbackMessage,
            isDark = themePalette.isDark,
            onBuyAttemptsAndContinue = onBuyAttemptsAndContinue,
            onBuyStayInSameLevel = onBuyStayInSameLevel,
            onRefillLives = onRefillLives,
            onRestartLevel = onRestartLevel
        )
    }
}
