package com.sophia.enciclopedia.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sophia.domain.ThemeMode

@Composable fun SophiaTheme(mode:ThemeMode,content:@Composable ()->Unit){
    val dark=when(mode){ThemeMode.SYSTEM->isSystemInDarkTheme();ThemeMode.DARK->true;ThemeMode.LIGHT->false}
    // Paleta moderna "céu e mar": azul profundo e azul-céu sobre branco gelo; cada tradição
    // mantém o seu símbolo e a sua ilustração a cores (ver TopicArt e BookCover).
    val colors=if(dark)darkColorScheme(primary=Color(0xFF9CC2FF),onPrimary=Color(0xFF0A2450),primaryContainer=Color(0xFF1D3A6B),onPrimaryContainer=Color(0xFFD7E6FF),secondary=Color(0xFF8FCBFF),onSecondary=Color(0xFF06283F),secondaryContainer=Color(0xFF17456B),onSecondaryContainer=Color(0xFFD2EBFF),tertiary=Color(0xFFB9BFFF),onTertiary=Color(0xFF23255C),background=Color(0xFF081226),onBackground=Color(0xFFE8F0FF),surface=Color(0xFF0F1F3D),onSurface=Color(0xFFE8F0FF),surfaceVariant=Color(0xFF1A2C4E),onSurfaceVariant=Color(0xFFB9CBE8),outline=Color(0xFF4A648F),outlineVariant=Color(0xFF24365A),error=Color(0xFFFFB4AB)) else lightColorScheme(primary=Color(0xFF1D4FA1),onPrimary=Color.White,primaryContainer=Color(0xFFD7E6FF),onPrimaryContainer=Color(0xFF0A2450),secondary=Color(0xFF1F6FBC),onSecondary=Color.White,secondaryContainer=Color(0xFFDFF1FF),onSecondaryContainer=Color(0xFF06355C),tertiary=Color(0xFF4A4FB0),onTertiary=Color.White,background=Color(0xFFF4F8FF),onBackground=Color(0xFF12233D),surface=Color.White,onSurface=Color(0xFF12233D),surfaceVariant=Color(0xFFE6EFFB),onSurfaceVariant=Color(0xFF44597A),outline=Color(0xFF93A7C9),outlineVariant=Color(0xFFC9D8EF),error=Color(0xFFBA1A1A))
    val base=Typography()
    val serif=FontFamily.Serif
    val type=Typography(displayLarge=base.displayLarge.copy(fontFamily=serif),displayMedium=base.displayMedium.copy(fontFamily=serif),displaySmall=base.displaySmall.copy(fontFamily=serif),headlineLarge=base.headlineLarge.copy(fontFamily=serif),headlineMedium=base.headlineMedium.copy(fontFamily=serif),headlineSmall=base.headlineSmall.copy(fontFamily=serif),titleLarge=base.titleLarge.copy(fontFamily=serif))
    val shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(32.dp))
    MaterialTheme(colorScheme=colors,typography=type,shapes=shapes,content=content)
}
