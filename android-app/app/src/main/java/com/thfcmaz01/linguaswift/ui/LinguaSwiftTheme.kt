package com.thfcmaz01.linguaswift.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun LinguaSwiftTheme(dark: Boolean, content: @Composable () -> Unit) {
    val light = lightColorScheme(primary = Color(0xFF2563EB), background = Color(0xFFF0F2F7),
        surface = Color.White, surfaceVariant = Color(0xFFF7F8FC), onSurface = Color(0xFF0F1423),
        onSurfaceVariant = Color(0xFF626F86), error = Color(0xFFB3261E))
    val darkColors = darkColorScheme(primary = Color(0xFF60A5FA), background = Color(0xFF0D1117),
        surface = Color(0xFF161B27), surfaceVariant = Color(0xFF1E2536), onSurface = Color(0xFFE8ECF4),
        onSurfaceVariant = Color(0xFFAFB9CB))
    MaterialTheme(colorScheme = if (dark) darkColors else light, content = content)
}
