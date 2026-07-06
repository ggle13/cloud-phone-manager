package com.example.cloudphone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── 品牌色 ───
object CloudColors {
    // 主色系
    val Primary = Color(0xFF1E88E5)      // 科技蓝
    val PrimaryDark = Color(0xFF1565C0)   // 深蓝
    val PrimaryLight = Color(0xFF64B5F6)  // 浅蓝
    val Accent = Color(0xFF00BCD4)        // 青色点缀

    // 状态色
    val StatusOnline = Color(0xFF4CAF50)   // 运行中 - 绿色
    val StatusOffline = Color(0xFF9E9E9E)  // 已关机 - 灰色
    val StatusPending = Color(0xFFFF9800)  // 处理中 - 橙色
    val StatusError = Color(0xFFF44336)    // 异常 - 红色

    // 渐变色（用于按钮高光）
    val GradientStart = Color(0xFF1E88E5)
    val GradientEnd = Color(0xFF42A5F5)

    // 暗色模式
    val DarkBg = Color(0xFF0D1117)        // 深色背景
    val DarkSurface = Color(0xFF161B22)    // 卡片背景
    val DarkCard = Color(0xFF21262D)       // 稍亮卡片

    // 浅色模式
    val LightBg = Color(0xFFF0F4F8)
    val LightSurface = Color(0xFFFFFFFF)
}

// ─── 亮色主题 ───
private val LightColorScheme = lightColorScheme(
    primary = CloudColors.Primary,
    onPrimary = Color.White,
    primaryContainer = CloudColors.PrimaryLight.copy(alpha = 0.15f),
    onPrimaryContainer = CloudColors.PrimaryDark,

    secondary = Color(0xFF546E7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECEFF1),
    onSecondaryContainer = Color(0xFF37474F),

    tertiary = CloudColors.Accent,
    onTertiary = Color.White,

    background = CloudColors.LightBg,
    onBackground = Color(0xFF1A1A2E),

    surface = CloudColors.LightSurface,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF5F7FA),
    onSurfaceVariant = Color(0xFF64748B),

    error = CloudColors.StatusError,
    onError = Color.White,

    outline = Color(0xFFE0E6ED),
    outlineVariant = Color(0xFFECEFF1),
)

// ─── 暗色主题 ───
private val DarkColorScheme = darkColorScheme(
    primary = CloudColors.PrimaryLight,
    onPrimary = CloudColors.DarkBg,
    primaryContainer = CloudColors.Primary.copy(alpha = 0.25f),
    onPrimaryContainer = CloudColors.PrimaryLight,

    secondary = Color(0xFF90A4AE),
    onSecondary = Color(0xFF1A1A2E),
    secondaryContainer = CloudColors.DarkCard,
    onSecondaryContainer = Color(0xFFB0BEC5),

    tertiary = CloudColors.Accent,
    onTertiary = Color.Black,

    background = CloudColors.DarkBg,
    onBackground = Color(0xFFE6EDF3),

    surface = CloudColors.DarkSurface,
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = CloudColors.DarkCard,
    onSurfaceVariant = Color(0xFF8B949E),

    error = CloudColors.StatusError,
    onError = Color.White,

    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF21262D),
)

// ─── Typography ───
private val CloudTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
        fontFamily = FontFamily.SansSerif
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
        fontFamily = FontFamily.SansSerif
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
        fontFamily = FontFamily.SansSerif
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        fontFamily = FontFamily.SansSerif
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        fontFamily = FontFamily.SansSerif
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        fontFamily = FontFamily.SansSerif
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        fontFamily = FontFamily.SansSerif
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        fontFamily = FontFamily.SansSerif
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
        fontFamily = FontFamily.SansSerif
    )
)

@Composable
fun CloudPhoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CloudTypography,
        content = content
    )
}
