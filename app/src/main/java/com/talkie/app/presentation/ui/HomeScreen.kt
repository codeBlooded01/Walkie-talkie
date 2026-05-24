package com.talkie.app.presentation.ui

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.talkie.app.domain.Contact
import java.util.Calendar

private fun timeGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11  -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else      -> "Good Evening"
    }
}

@Composable
fun HomeScreen(
    deviceName       : String,
    devicePhotoUri   : String?,
    contacts         : List<Contact>,
    receivingFrom    : String?,   // contactId of the peer currently sending audio
    incomingAmplitude: Float,     // normalised RMS of incoming audio [0f,1f]
    onAvatarClick    : () -> Unit,
    onMenuClick      : () -> Unit,
    onContactAvatar  : (Contact) -> Unit,
    onTalk           : (Contact) -> Unit,
    onSync           : (Contact) -> Unit
) {
    val screen   = rememberScreenDimensions()
    val paired   = contacts.filter { it.isPaired }
    val available = contacts.filter { !it.isPaired }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF7F8FC))
            .padding(top = screen.hp(0.025f), start = screen.wp(0.05f), end = screen.wp(0.05f))
    ) {
        // ── Top greeting bar ──────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactAvatar(devicePhotoUri, size = screen.wp(0.13f), onClick = onAvatarClick)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Hi ${deviceName.split(" ").first()}", fontFamily = LeagueSpartan, fontSize = 16.sp, color = GrayText)
                    Text(timeGreeting(), fontFamily = Montserrat, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
                }
            }
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = NavyBlue,
                modifier = Modifier.size(screen.wp(0.07f)).clickable { onMenuClick() })
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (paired.isNotEmpty()) {
                item {
                    Text("Paired Device", fontFamily = Montserrat, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = NavyBlue)
                    Spacer(Modifier.height(8.dp))
                }
                items(paired) { c ->
                    val isSpeaking = receivingFrom == c.id
                    ContactRow(
                        contact           = c,
                        buttonText        = "TALK",
                        isReceiving       = isSpeaking,
                        incomingAmplitude = if (isSpeaking) incomingAmplitude else 0f,
                        onAvatarClick     = { onContactAvatar(c) },
                        onButtonClick     = { onTalk(c) }
                    )
                }
            }
            if (available.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("Available Devices", fontFamily = Montserrat, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = NavyBlue)
                    Spacer(Modifier.height(8.dp))
                }
                items(available) { c ->
                    ContactRow(
                        contact       = c,
                        buttonText    = "Sync",
                        onAvatarClick = { onContactAvatar(c) },
                        onButtonClick = { onSync(c) }
                    )
                }
            }
        }
    }
}
