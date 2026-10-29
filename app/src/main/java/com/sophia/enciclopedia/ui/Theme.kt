package com.sophia.enciclopedia.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.sophia.domain.ThemeMode

val SophiaGold=Color(0xFFC6A767)
@Composable fun SophiaTheme(mode:ThemeMode,content:@Composable ()->Unit){
    val dark=when(mode){ThemeMode.SYSTEM->isSystemInDarkTheme();ThemeMode.DARK->true;ThemeMode.LIGHT->false}
    val colors=if(dark)darkColorScheme(primary=Color(0xFFB7C4FF),onPrimary=Color(0xFF172A66),primaryContainer=Color(0xFF2D3D70),onPrimaryContainer=Color(0xFFDFE5FF),secondary=Color(0xFFE2C88D),onSecondary=Color(0xFF3B2D0C),secondaryContainer=Color(0xFF4A3D24),onSecondaryContainer=Color(0xFFF7E6C0),tertiary=Color(0xFFD1BCF5),background=Color(0xFF0F1423),onBackground=Color(0xFFE8EAF4),surface=Color(0xFF171E30),onSurface=Color(0xFFE8EAF4),surfaceVariant=Color(0xFF293249),onSurfaceVariant=Color(0xFFBDC5D9),outlineVariant=Color(0xFF3C465F)) else lightColorScheme(primary=Color(0xFF384D89),onPrimary=Color.White,primaryContainer=Color(0xFFE0E6FF),onPrimaryContainer=Color(0xFF162453),secondary=Color(0xFF765B21),onSecondary=Color.White,secondaryContainer=Color(0xFFF2E4C4),onSecondaryContainer=Color(0xFF3E2E09),tertiary=Color(0xFF72578E),background=Color(0xFFF8F7FC),onBackground=Color(0xFF1C2435),surface=Color.White,onSurface=Color(0xFF1C2435),surfaceVariant=Color(0xFFE9ECF5),onSurfaceVariant=Color(0xFF4F5B72),outlineVariant=Color(0xFFD2D8E7))
    val base=Typography()
    val type=Typography(headlineLarge=base.headlineLarge.copy(fontFamily=FontFamily.Serif),headlineMedium=base.headlineMedium.copy(fontFamily=FontFamily.Serif),headlineSmall=base.headlineSmall.copy(fontFamily=FontFamily.Serif))
    MaterialTheme(colorScheme=colors,typography=type,content=content)
}
