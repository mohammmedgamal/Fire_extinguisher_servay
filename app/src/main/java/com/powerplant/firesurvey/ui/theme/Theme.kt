package com.powerplant.firesurvey.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OkGreen = Color(0xFF2E7D32)
val NotOkRed = Color(0xFFC62828)
val WarningAmber = Color(0xFFEF6C00)
val NeutralGrey = Color(0xFF757575)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC62828),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF455A64),
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF690002),
    primaryContainer = Color(0xFF930005),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFB0BEC5),
)

@Composable
fun FireSurveyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
