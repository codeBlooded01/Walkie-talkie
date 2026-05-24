package com.talkie.app.presentation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.talkie.app.R

val NavyBlue   = Color(0xFF001A49)
val NavyLight  = Color(0xFF001A49).copy(alpha = 0.08f)
val PurpleMic  = Color(0xFF3B106A)
val PurpleGlow = Color(0x665E2A99)
val GrayText   = Color(0xFF9E9E9E)
val CardFill   = Color(0xFFF4F1F1)

val LeagueSpartan = FontFamily(
    Font(R.font.league_spartan_light, FontWeight.Light),
    Font(R.font.league_spartan_regular, FontWeight.Normal),
    Font(R.font.league_spartan_bold, FontWeight.Bold)
)

val KonkhmerSleokchher = FontFamily(
    Font(R.font.konkhmer_sleokchher_regular, FontWeight.Normal)
)

val Montserrat = FontFamily(
    Font(R.font.montserrat_bold, FontWeight.Bold)
)

val Koulen = FontFamily(
    Font(R.font.koulen_regular, FontWeight.Normal)
)

@androidx.compose.runtime.Composable
fun rememberScreenDimensions(): ScreenDimensions {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    return ScreenDimensions(config.screenWidthDp, config.screenHeightDp)
}

data class ScreenDimensions(val w: Int, val h: Int) {
    fun wp(f: Float) = (w * f).dp
    fun hp(f: Float) = (h * f).dp
}
