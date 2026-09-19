@file:OptIn(ExperimentalTextApi::class)

package ru.finny.petgame.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.finny.petgame.R

val NunitoFontFamily = FontFamily(
    Font(R.font.nunito, weight = FontWeight.Normal),
    Font(
        R.font.nunito,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.nunito,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.nunito,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        R.font.nunito,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)

private val NunitoStyle = TextStyle(fontFamily = NunitoFontFamily)

val FinnyTypography = Typography(
    displayLarge = NunitoStyle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    displayMedium = NunitoStyle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    displaySmall = NunitoStyle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineLarge = NunitoStyle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = NunitoStyle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = NunitoStyle.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = NunitoStyle.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = NunitoStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = NunitoStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = NunitoStyle.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = NunitoStyle.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = NunitoStyle.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = NunitoStyle.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = NunitoStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    labelSmall = NunitoStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
)