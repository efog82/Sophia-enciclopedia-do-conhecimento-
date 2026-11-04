package com.sophia.enciclopedia.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sophia.domain.ThemeMode

val SophiaGold=Color(0xFFC6A767)
@Composable fun SophiaTheme(mode:ThemeMode,content:@Composable ()->Unit){
    val dark=when(mode){ThemeMode.SYSTEM->isSystemInDarkTheme();ThemeMode.DARK->true;ThemeMode.LIGHT->false}
    // Paleta moderna "papel e tinta": fundo quente, azul-tinta profundo e dourado como acento.
    val colors=if(dark)darkColorScheme(primary=Color(0xFFAFC2FF),onPrimary=Color(0xFF16264D),primaryContainer=Color(0xFF22345C),onPrimaryContainer=Color(0xFFDCE4FF),secondary=Color(0xFFE3C878),onSecondary=Color(0xFF3A2C07),secondaryContainer=Color(0xFF4A3B16),onSecondaryContainer=Color(0xFFF7E7BC),tertiary=Color(0xFFD8BFE8),onTertiary=Color(0xFF3A244D),background=Color(0xFF131110),onBackground=Color(0xFFEFE9DC),surface=Color(0xFF1D1A16),onSurface=Color(0xFFEFE9DC),surfaceVariant=Color(0xFF2B261E),onSurfaceVariant=Color(0xFFCFC5AE),outline=Color(0xFF57503F),outlineVariant=Color(0xFF3B362A),error=Color(0xFFFFB4AB)) else lightColorScheme(primary=Color(0xFF2E4372),onPrimary=Color.White,primaryContainer=Color(0xFFDCE4F7),onPrimaryContainer=Color(0xFF12234A),secondary=Color(0xFF8C6D1F),onSecondary=Color.White,secondaryContainer=Color(0xFFF5E7C6),onSecondaryContainer=Color(0xFF3E2E09),tertiary=Color(0xFF6A4E8C),onTertiary=Color.White,background=Color(0xFFF8F5EF),onBackground=Color(0xFF1D2026),surface=Color(0xFFFFFDF8),onSurface=Color(0xFF1D2026),surfaceVariant=Color(0xFFEDE8DC),onSurfaceVariant=Color(0xFF57503F),outline=Color(0xFF8A8271),outlineVariant=Color(0xFFD8D2C2),error=Color(0xFFBA1A1A))
    val base=Typography()
    val serif=FontFamily.Serif
    val type=Typography(displayLarge=base.displayLarge.copy(fontFamily=serif),displayMedium=base.displayMedium.copy(fontFamily=serif),displaySmall=base.displaySmall.copy(fontFamily=serif),headlineLarge=base.headlineLarge.copy(fontFamily=serif),headlineMedium=base.headlineMedium.copy(fontFamily=serif),headlineSmall=base.headlineSmall.copy(fontFamily=serif),titleLarge=base.titleLarge.copy(fontFamily=serif))
    val shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(32.dp))
    MaterialTheme(colorScheme=colors,typography=type,shapes=shapes,content=content)
}
