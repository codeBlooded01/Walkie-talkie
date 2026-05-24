package com.talkie.app.presentation.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.talkie.app.domain.Contact

@Composable
fun PttScreen(
    contact       : Contact,
    isTransmitting: Boolean,
    onPress       : () -> Unit,
    onRelease     : () -> Unit,
    onMenuClick   : () -> Unit
) {
    val screen = rememberScreenDimensions()
    var isVoiceChangerOpen by remember { mutableStateOf(false) }
    var selectedVoice by remember { mutableStateOf("Robot") } // Highlight robot by default

    if (isVoiceChangerOpen) {
        Dialog(onDismissRequest = { isVoiceChangerOpen = false }) {
            Card(
                modifier = Modifier.width(280.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voice Changer", fontFamily = KonkhmerSleokchher, fontSize = 22.sp, color = NavyBlue)
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NavyBlue,
                            modifier = Modifier.size(24.dp).clickable { isVoiceChangerOpen = false }
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("Male", Icons.Default.Person, "Male"),
                            Triple("Robot", Icons.Default.Android, "Robot"),
                            Triple("Female", Icons.Default.Face, "Female")
                        ).forEach { (name, icon, label) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedVoice = name }
                                    .padding(8.dp)
                            ) {
                                val isSelected = selectedVoice == name
                                val tint = if (isSelected) NavyBlue else NavyBlue.copy(alpha = 0.5f)
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = tint,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    label,
                                    fontFamily = LeagueSpartan,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 14.sp,
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    Box(Modifier.fillMaxSize()) {
        // ── Full-screen background photo ──────────────────────────────────────
        if (!contact.photoUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(contact.photoUri), contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFFB0CCE8)))
        }

        // ── Semi-transparent overlay for legibility ───────────────────────────
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))

        Column(
            Modifier.fillMaxSize().padding(top = screen.hp(0.025f), bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth().padding(horizontal = screen.wp(0.05f)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White,
                    modifier = Modifier.size(screen.wp(0.09f)).clickable { onMenuClick() })
                Icon(Icons.Default.Tune, contentDescription = "Settings", tint = Color.White,
                    modifier = Modifier.size(screen.wp(0.07f)).clickable { isVoiceChangerOpen = true })
            }

            Spacer(Modifier.weight(0.5f))

            // ── Large circular avatar with white ring ─────────────────────────
            Box(
                Modifier.size(screen.wp(0.60f).coerceAtMost(240.dp)).border(6.dp, Color.White, CircleShape)
                    .padding(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!contact.photoUri.isNullOrBlank()) {
                    AsyncImage(Uri.parse(contact.photoUri), null,
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Contact name + status ─────────────────────────────────────────
            Text(contact.name, fontFamily = LeagueSpartan, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isTransmitting) "Listening..." else "Press to speak",
                fontFamily = LeagueSpartan, fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.weight(1f))

            // ── Waveform (when transmitting) ──────────────────────────────────
            if (isTransmitting) {
                SoundWaveAnimation(color = Color.White)
                Spacer(Modifier.height(28.dp))
            } else {
                Spacer(Modifier.height(80.dp))
            }

            // ── Mic PTT button ────────────────────────────────────────────────
            Box(
                Modifier.size(screen.wp(0.35f).coerceAtMost(140.dp))
                    .background(if (isTransmitting) PurpleGlow else Color.Transparent, CircleShape)
                    .padding(16.dp).clip(CircleShape)
                    .background(if (isTransmitting) PurpleMic else NavyBlue)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            onPress()
                            tryAwaitRelease()
                            onRelease()
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = "PTT",
                    tint = Color.White, modifier = Modifier.size(screen.wp(0.14f)))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
