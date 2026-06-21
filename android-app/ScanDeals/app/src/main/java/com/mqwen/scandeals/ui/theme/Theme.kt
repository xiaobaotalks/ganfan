package com.mqwen.scandeals.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 干饭省省 主题:暖橙 + 米色背景
 */
val ScanDealsColors = lightColorScheme(
    primary = Color(0xFFFF9966),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0CC),
    onPrimaryContainer = Color(0xFF1F1F1F),
    secondary = Color(0xFFFFB892),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF1F1F1F),
    surface = Color.White,
    onSurface = Color(0xFF1F1F1F),
    error = Color(0xFFD32F2F)
)

val ScanDealsShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val ScanDealsTypography = Typography(
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun ScanDealsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ScanDealsColors,
        shapes = ScanDealsShapes,
        typography = ScanDealsTypography,
        content = content
    )
}
