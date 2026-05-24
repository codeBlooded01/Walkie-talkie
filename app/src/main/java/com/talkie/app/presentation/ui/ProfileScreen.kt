package com.talkie.app.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

@Composable
fun ProfileScreen(
    name        : String,
    bio         : String,
    photoUri    : String?,
    ipAddress   : String,
    isOwnProfile: Boolean = false,
    onBack      : () -> Unit,
    onTalk      : (() -> Unit)? = null,
    onNameSave  : ((String) -> Unit)? = null,
    onPhotoSave : ((String) -> Unit)? = null
) {
    val screen = rememberScreenDimensions()
    val context = LocalContext.current
    var isEditingName by remember { mutableStateOf(false) }
    
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onPhotoSave?.invoke(it.toString())
        }
    }

    if (isEditingName) {
        var newName by remember { mutableStateOf(name) }
        Dialog(onDismissRequest = { isEditingName = false }) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White)) {
                Column(Modifier.padding(screen.wp(0.06f)), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(screen.hp(0.018f))) {
                    Text(
                        text = "Edit Name", 
                        fontFamily = KonkhmerSleokchher, 
                        fontSize = 22.sp, 
                        color = NavyBlue,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyBlue, unfocusedBorderColor = Color.LightGray),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = LeagueSpartan, fontWeight = FontWeight.Light, fontSize = 18.sp, color = NavyBlue.copy(alpha=0.6f))
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        NavyPillButton(
                            text = "Save",
                            modifier = Modifier.width(85.dp),
                            onClick = { 
                                onNameSave?.invoke(newName.trim())
                                isEditingName = false
                            }
                        )
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        // ── Banner with Gradient Fade ─────────────────────────────────────────
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.45f)) {
            if (!photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(photoUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFFB0CCE8)))
            }
            
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1.0f to Color.White
                        )
                    )
            )
        }

        // ── Top Icons ─────────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = screen.hp(0.025f), start = screen.wp(0.06f), end = screen.wp(0.06f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "Back",
                tint = NavyBlue,
                modifier = Modifier
                    .size(screen.wp(0.09f))
                    .clickable { onBack() }
            )
            if (!isOwnProfile) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = NavyBlue,
                    modifier = Modifier
                        .size(screen.wp(0.07f))
                        .clickable { onBack() }
                )
            } else {
                Spacer(Modifier.size(screen.wp(0.07f)))
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.fillMaxHeight(0.35f))

            // Avatar
            Box(
                Modifier
                    .size(screen.wp(0.50f).coerceAtMost(210.dp))
                    .border(5.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .background(NavyBlue.copy(0.1f))
                    .clickable(enabled = isOwnProfile) { photoLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (!photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(photoUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (isOwnProfile) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Photo", tint = NavyBlue, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(screen.hp(0.03f)))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontFamily = LeagueSpartan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = NavyBlue,
                    modifier = Modifier.clickable(enabled = isOwnProfile) { isEditingName = true }
                )
                if (isOwnProfile) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = NavyBlue, modifier = Modifier.size(16.dp).clickable { isEditingName = true })
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = bio,
                fontFamily = LeagueSpartan,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                color = Color.Black.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = screen.wp(0.10f))
            )

            Spacer(Modifier.weight(1f))

            // ── Stats Card with overlapping ONLINE button ──────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screen.wp(0.05f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardFill),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = (38.dp / 2)) // overlap
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatCell("UDP Active", "Direct Stream")
                        Divider(Modifier.width(1.dp).height(32.dp), color = Color.Gray.copy(alpha = 0.3f))
                        StatCell(ipAddress.ifBlank { "N/A" }, "Node IP")
                        Divider(Modifier.width(1.dp).height(32.dp), color = Color.Gray.copy(alpha = 0.3f))
                        StatCell("Latency Low", "Status")
                    }
                }

                Button(
                    onClick = { onTalk?.invoke() },
                    modifier = Modifier.width(100.dp).height(38.dp),
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Online", fontFamily = LeagueSpartan, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(screen.hp(0.08f)))
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = LeagueSpartan,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = NavyBlue,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            )
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            fontFamily = LeagueSpartan,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = NavyBlue.copy(alpha=0.7f),
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
