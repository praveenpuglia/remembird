package com.praveenpuglia.remembird.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.praveenpuglia.remembird.R
import com.praveenpuglia.remembird.Season

/** The scrapbook: kraft paper, ink, a pink highlighter and a red pen. Always light, like real paper. */
object Paper {
    val kraft = Color(0xFFF3EAD8)
    val speck = Color(0x1A785A28) // the paper's fibre dots
    val ink = Color(0xFF3B2F2F)
    val card = Color(0xFFFFFDF7)
    val highlighter = Color(0xFFF7A1B5)
    val pen = Color(0xFFD64545)
}

private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Caveat: handwriting for names, months and the title. */
val Handwriting = FontFamily(variable(R.font.caveat, 600), variable(R.font.caveat, 700))

private val Nunito = FontFamily(variable(R.font.nunito, 400), variable(R.font.nunito, 600), variable(R.font.nunito, 700))

@Composable
fun RemembirdTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Paper.ink,
        onPrimary = Paper.card,
        secondaryContainer = Paper.highlighter,
        onSecondaryContainer = Paper.ink,
        background = Paper.kraft,
        onBackground = Paper.ink,
        surface = Paper.kraft,
        onSurface = Paper.ink,
        onSurfaceVariant = Paper.ink.copy(alpha = 0.72f),
        outline = Paper.ink,
    )
    MaterialTheme(colorScheme = colors, typography = Typography().withFontFamily(Nunito), content = content)
}

private fun Typography.withFontFamily(fontFamily: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)

/** Each ritu's ink on the timeline and its washi tape. */
fun Season.color() = when (this) {
    Season.SHISHIR -> Color(0xFF6FA8DC) // icy blue
    Season.VASANT -> Color(0xFFE8799B) // blossom pink
    Season.GRISHMA -> Color(0xFFE9A23B) // sun amber
    Season.VARSHA -> Color(0xFF3FA796) // rain-washed teal
    Season.SHARAD -> Color(0xFF9479C9) // clear-night violet
    Season.HEMANT -> Color(0xFFB8934A) // harvest gold
}

/**
 * Diagonal two-tone stripes in the season's colour, like a strip of washi tape. Both tones are pale and
 * close together so the stripes read as texture and the handwritten month on top stays legible.
 */
fun Season.tape(stripePx: Float): Brush {
    val stripe = lerp(color(), Color.White, 0.45f)
    val ground = lerp(color(), Color.White, 0.68f)
    return Brush.linearGradient(
        0f to stripe, 0.5f to stripe, 0.5f to ground, 1f to ground,
        start = Offset.Zero, end = Offset(stripePx, stripePx), tileMode = TileMode.Repeated,
    )
}
