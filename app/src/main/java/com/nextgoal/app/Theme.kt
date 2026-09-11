package com.nextgoal.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NextGoalBackground = Color(0xFF050609)
val NextGoalSurface = Color(0xFF11141C)
val NextGoalSurfaceRaised = Color(0xFF171B25)
val NextGoalCyan = Color(0xFF08D7FF)
val NextGoalBlue = Color(0xFF0B75FF)
val NextGoalText = Color(0xFFF5F7FA)
val NextGoalMuted = Color(0xFF8993A6)
val NextGoalLine = Color(0xFF202633)
val NextGoalRed = Color(0xFFFF4650)
val NextGoalYellow = Color(0xFFFFD429)

private val NextGoalColors = darkColorScheme(
    primary = NextGoalCyan,
    onPrimary = Color(0xFF00151B),
    secondary = NextGoalBlue,
    background = NextGoalBackground,
    onBackground = NextGoalText,
    surface = NextGoalSurface,
    onSurface = NextGoalText,
    surfaceVariant = NextGoalSurfaceRaised,
    onSurfaceVariant = NextGoalMuted,
    outline = NextGoalLine
)

private val NextGoalTypography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 29.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.ExtraBold
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 22.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.ExtraBold
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 19.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Bold
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Bold
    )
)

@Composable
fun NextGoalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NextGoalColors,
        typography = NextGoalTypography,
        content = content
    )
}
