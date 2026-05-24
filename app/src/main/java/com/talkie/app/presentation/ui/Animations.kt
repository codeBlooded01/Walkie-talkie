package com.talkie.app.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Full-size soundwave for PttScreen (idle animation) ────────────────────────
@Composable
fun SoundWaveAnimation(color: Color = NavyBlue) {
    val inf = rememberInfiniteTransition(label = "wave")
    val heights = (0 until 9).map { i ->
        inf.animateFloat(
            initialValue = 0.25f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(400, delayMillis = i * 80, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "b$i"
        )
    }
    Row(
        modifier = Modifier.height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            val v by h
            Box(
                Modifier.width(6.dp).fillMaxHeight(v)
                    .clip(RoundedCornerShape(3.dp)).background(color)
            )
        }
    }
}

// ── Mini soundwave: idle-animated, used in contact rows when no amplitude data ─
@Composable
fun MiniWaveAnimation(color: Color = NavyBlue) {
    val inf = rememberInfiniteTransition(label = "miniwave")
    val heights = (0 until 7).map { i ->
        inf.animateFloat(
            initialValue = 0.25f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(350, delayMillis = i * 70, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "mb$i"
        )
    }
    Row(
        modifier = Modifier.height(32.dp).padding(end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            val v by h
            Box(
                Modifier.width(4.dp).fillMaxHeight(v)
                    .clip(RoundedCornerShape(2.dp)).background(color)
            )
        }
    }
}

/**
 * Amplitude-reactive soundwave that directly maps incoming RMS [0f,1f] to bar heights.
 *
 * Each bar tracks [amplitude] with a small spring animation for smooth decay.
 * Bar heights are staggered with a sine-wave envelope so the waveform looks organic,
 * exactly matching the design in the reference image.
 *
 * This is what appears in the ContactRow when a peer is actively speaking.
 *
 * @param amplitude Normalised incoming RMS amplitude [0f,1f].
 * @param color     Bar colour (defaults to NavyBlue).
 * @param barCount  Number of bars (default 11 to match design).
 * @param maxHeight Maximum bar height in dp.
 */
@Composable
fun AmplitudeWaveAnimation(
    amplitude : Float,
    color     : Color = NavyBlue,
    barCount  : Int   = 9,
    maxHeight : Dp    = 32.dp
) {
    val smoothedAmplitude by animateFloatAsState(
        targetValue    = amplitude,
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "ampSmooth"
    )

    val inf = rememberInfiniteTransition(label = "miniwave")
    val heights = (0 until barCount).map { i ->
        inf.animateFloat(
            initialValue = 0.25f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(400, delayMillis = i * 70, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "mb$i"
        )
    }

    Row(
        modifier            = Modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment   = Alignment.CenterVertically
    ) {
        repeat(barCount) { i ->
            val envelope = kotlin.math.sin(Math.PI * i / (barCount - 1)).toFloat()
                .coerceIn(0.35f, 1.0f)

            val rawHeight = heights[i].value
            val fraction = (0.2f + (rawHeight - 0.2f) * smoothedAmplitude * envelope).coerceIn(0.15f, 1.0f)

            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(color)
            )
        }
    }
}
