package com.talkie.app.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.talkie.app.domain.Contact

// ── Reusable circular avatar ──────────────────────────────────────────────────
@Composable
fun ContactAvatar(photoUri: String?, size: Dp, onClick: (() -> Unit)? = null) {
    val mod = Modifier
        .size(size)
        .clip(CircleShape)
        .background(NavyBlue.copy(alpha = 0.12f))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    Box(mod, contentAlignment = Alignment.Center) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(photoUri), contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.Person, contentDescription = null,
                tint = NavyBlue, modifier = Modifier.size(size * 0.55f))
        }
    }
}

// ── Navy pill button ──────────────────────────────────────────────────────────
@Composable
fun NavyPillButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
        shape = RoundedCornerShape(19.dp), 
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.height(38.dp)
    ) { 
        Text(
            text = text, 
            fontFamily = LeagueSpartan, 
            fontWeight = FontWeight.Bold, 
            fontSize = 18.sp, // Increased from 16.sp
            color = Color.White,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        ) 
    }
}

// ── Contact row in home list ──────────────────────────────────────────────────
@Composable
fun ContactRow(
    contact        : Contact,
    buttonText     : String,
    isReceiving    : Boolean = false,
    /** Normalised incoming RMS amplitude [0f,1f]. Only meaningful when isReceiving=true. */
    incomingAmplitude: Float  = 0f,
    onAvatarClick  : () -> Unit,
    onButtonClick  : () -> Unit
) {
    val screen = rememberScreenDimensions()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(screen.wp(0.04f)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(screen.wp(0.03f)).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactAvatar(contact.photoUri, size = screen.wp(0.13f), onClick = onAvatarClick)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        contact.name,
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A1B4D),
                        fontSize = 20.sp
                    )
                    if (isReceiving) {
                        // ── "is speaking..." label ─────────────────────────────
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "is speaking...",
                                fontFamily = LeagueSpartan,
                                color = Color(0xFF5D6B82),
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                            Spacer(Modifier.width(4.dp))
                            Text("In-sync", fontFamily = LeagueSpartan, color = GrayText, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (isReceiving) {
                // ── Amplitude-reactive waveform replaces Talk button ─────────
                AmplitudeWaveAnimation(
                    amplitude = incomingAmplitude,
                    color     = NavyBlue
                )
            } else {
                NavyPillButton(buttonText, modifier = Modifier.width(80.dp), onClick = onButtonClick)
            }
        }
    }
}

// ── Settings bottom sheet ─────────────────────────────────────────────────────
@Composable
fun SettingsSheet(
    onAddContact: () -> Unit,
    onEditContact: () -> Unit,
    onDeleteContact: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { onDismiss() }) {
        Card(
            modifier = Modifier.align(Alignment.Center),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.padding(24.dp).width(270.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Settings", 
                        fontFamily = KonkhmerSleokchher, 
                        fontSize = 22.sp, 
                        color = NavyBlue,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NavyBlue, modifier = Modifier.clickable { onDismiss() })
                }
                Spacer(Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Add Contact" to onAddContact, "Edit Contact" to onEditContact, "Delete Contact" to onDeleteContact, "Logout" to onLogout)
                        .forEach { (label, action) ->
                            Button(
                                onClick = { action() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(11.dp), 
                                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                            ) { 
                                Text(
                                    text = label, 
                                    fontFamily = LeagueSpartan, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 20.sp, // Increased from 18.sp
                                    color = Color.White,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                ) 
                            }
                        }
                }
            }
        }
    }
}

// ── Add Contact dialog ────────────────────────────────────────────────────────
@Composable
fun AddContactDialog(onSave: (name: String, ip: String, uri: String?) -> Unit, onDismiss: () -> Unit) {
    val screen = rememberScreenDimensions()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            photoUri = it.toString()
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(screen.wp(0.85f)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(screen.wp(0.06f)), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(screen.hp(0.018f))) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                        Text(
                            text = "Add Contact", 
                            fontFamily = KonkhmerSleokchher, 
                            fontSize = 22.sp, 
                            color = NavyBlue,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 22.sp
                            )
                        )
                        Text(
                            text = "Enter Contact Details", 
                            fontFamily = LeagueSpartan, 
                            fontWeight = FontWeight.Light, 
                            color = NavyBlue.copy(alpha=0.6f), 
                            fontSize = 14.sp,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 14.sp
                            )
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NavyBlue, modifier = Modifier.clickable { onDismiss() })
                }
                
                Box(Modifier.size(screen.wp(0.42f)).clip(CircleShape).clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center) {
                    if (!photoUri.isNullOrBlank()) {
                        AsyncImage(Uri.parse(photoUri), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(NavyBlue), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Upload", tint = Color.White, modifier = Modifier.size(screen.wp(0.12f)))
                        }
                    }
                    Box(Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape)
                        .background(NavyBlue), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                
                Text("Upload Photo", fontFamily = LeagueSpartan, fontWeight = FontWeight.Light, color = NavyBlue.copy(alpha=0.6f), fontSize = 14.sp)
                
                OutlinedTextField(value = name, onValueChange = { name = it },
                    placeholder = { Text("Contact Name", fontFamily = LeagueSpartan, fontWeight = FontWeight.Light, fontSize = 18.sp, color = NavyBlue.copy(alpha=0.6f)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = LeagueSpartan, fontWeight = FontWeight.Light, fontSize = 18.sp, color = NavyBlue),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyBlue, unfocusedBorderColor = Color.LightGray))
                    
                OutlinedTextField(value = ip, onValueChange = { ip = it },
                    placeholder = { Text("Enter Peer's IP Address", fontFamily = LeagueSpartan, fontWeight = FontWeight.Light, fontSize = 18.sp, color = NavyBlue.copy(alpha=0.6f)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = LeagueSpartan, fontWeight = FontWeight.Light, fontSize = 18.sp, color = NavyBlue),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyBlue, unfocusedBorderColor = Color.LightGray))
                    
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    NavyPillButton("Save", modifier = Modifier.width(85.dp)) {
                        if (name.isNotBlank() && ip.isNotBlank()) onSave(name.trim(), ip.trim(), photoUri)
                    }
                }
            }
        }
    }
}

// ── Edit Contact dialog (contact selection + edit form) ───────────────────────
@Composable
fun EditContactDialog(contacts: List<Contact>, onSave: (id: String, name: String, ip: String, uri: String?) -> Unit, onDismiss: () -> Unit) {
    val screen = rememberScreenDimensions()
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Contact?>(null) }
    val c = selected
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(screen.wp(0.85f)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White)) {
            if (c == null) {
                // Selection list
                Column(Modifier.padding(screen.wp(0.06f)).heightIn(max = screen.hp(0.6f))) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                            Text(
                                text = "Edit Contact", 
                                fontFamily = KonkhmerSleokchher, 
                                fontSize = 22.sp, 
                                color = NavyBlue,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 22.sp
                                )
                            )
                            Text(
                                text = "Choose Contact to Edit", 
                                fontFamily = LeagueSpartan, 
                                fontWeight = FontWeight.Light, 
                                color = NavyBlue.copy(alpha=0.6f), 
                                fontSize = 14.sp,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 14.sp
                                )
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NavyBlue, modifier = Modifier.clickable { onDismiss() })
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(contacts) { contact ->
                            Row(Modifier.fillMaxWidth().clickable { selected = contact }
                                .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ContactAvatar(contact.photoUri, size = screen.wp(0.11f))
                                    Spacer(Modifier.width(12.dp))
                                    Text(contact.name, fontFamily = LeagueSpartan, fontWeight = FontWeight.Medium, color = NavyBlue, fontSize = 18.sp)
                                }
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyBlue)
                            }
                            Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                        }
                    }
                }
            } else {
                // Edit form
                var name by remember { mutableStateOf(c.name) }
                var ip by remember { mutableStateOf(c.ipAddress) }
                var photoUri by remember { mutableStateOf(c.photoUri) }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    uri?.let {
                        context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        photoUri = it.toString()
                    }
                }
                
                Column(Modifier.padding(screen.wp(0.06f)), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(screen.hp(0.018f))) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                            Text(
                                text = "Edit Contact", 
                                fontFamily = KonkhmerSleokchher, 
                                fontSize = 22.sp, 
                                color = NavyBlue,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 22.sp
                                )
                            )
                            Text(
                                text = "Edit Friend's Contact Details", 
                                fontFamily = LeagueSpartan, 
                                fontWeight = FontWeight.Light, 
                                color = NavyBlue.copy(alpha=0.6f), 
                                fontSize = 14.sp,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 14.sp
                                )
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NavyBlue, modifier = Modifier.clickable { onDismiss() })
                    }
                    Box(Modifier.size(screen.wp(0.42f)).clip(CircleShape).clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center) {
                        ContactAvatar(photoUri, size = screen.wp(0.42f))
                        Box(Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape)
                            .background(NavyBlue), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text("Update Photo", fontFamily = LeagueSpartan, fontWeight = FontWeight.Light, color = NavyBlue.copy(alpha=0.6f), fontSize = 14.sp)
                    
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = LeagueSpartan, fontWeight = FontWeight.Light, fontSize = 18.sp, color = NavyBlue),
                        trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NavyBlue) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyBlue, unfocusedBorderColor = Color.LightGray))
                        
                    OutlinedTextField(value = ip, onValueChange = { ip = it },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = LeagueSpartan, fontWeight = FontWeight.Light, fontSize = 18.sp, color = NavyBlue),
                        trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NavyBlue) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyBlue, unfocusedBorderColor = Color.LightGray))
                        
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        NavyPillButton("Save", modifier = Modifier.width(85.dp)) {
                            onSave(c.id, name.trim(), ip.trim(), photoUri)
                        }
                    }
                }
            }
        }
    }
}

// ── Delete Contact dialog (selection + confirm) ───────────────────────────────
@Composable
fun DeleteContactDialog(contacts: List<Contact>, onDelete: (id: String) -> Unit, onDismiss: () -> Unit) {
    val screen = rememberScreenDimensions()
    var toDelete by remember { mutableStateOf<Contact?>(null) }
    val c = toDelete
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(screen.wp(0.85f)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White)) {
            if (c == null) {
                Column(Modifier.padding(screen.wp(0.06f)).heightIn(max = screen.hp(0.6f))) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                            Text(
                                text = "Delete Contact", 
                                fontFamily = KonkhmerSleokchher, 
                                fontSize = 22.sp, 
                                color = NavyBlue,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 22.sp
                                )
                            )
                            Text(
                                text = "Choose Contact to Delete", 
                                fontFamily = LeagueSpartan, 
                                fontWeight = FontWeight.Light, 
                                color = NavyBlue.copy(alpha=0.6f), 
                                fontSize = 14.sp,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 14.sp
                                )
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NavyBlue, modifier = Modifier.clickable { onDismiss() })
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(contacts) { contact ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ContactAvatar(contact.photoUri, size = screen.wp(0.11f))
                                    Spacer(Modifier.width(12.dp))
                                    Text(contact.name, fontFamily = LeagueSpartan, fontWeight = FontWeight.Medium, color = NavyBlue, fontSize = 18.sp)
                                }
                                IconButton(onClick = { toDelete = contact }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NavyBlue)
                                }
                            }
                            Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(screen.wp(0.06f)), 
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Confirm Deletion", 
                            fontFamily = KonkhmerSleokchher, 
                            fontSize = 22.sp, 
                            color = NavyBlue,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NavyBlue,
                            modifier = Modifier.clickable { onDismiss() }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ContactAvatar(c.photoUri, size = screen.wp(0.23f))
                        Spacer(Modifier.height(8.dp))
                        Text(c.name, fontFamily = LeagueSpartan, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = NavyBlue)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = c.ipAddress, 
                            fontFamily = LeagueSpartan, 
                            fontWeight = FontWeight.Normal, 
                            color = GrayText, 
                            fontSize = 15.sp,
                            textDecoration = TextDecoration.Underline
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Confirm deletion? This contact\nwill be permanently removed.",
                            fontFamily = LeagueSpartan, 
                            fontWeight = FontWeight.Light, 
                            color = NavyBlue.copy(alpha=0.6f), 
                            fontSize = 15.sp, 
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(19.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, NavyBlue)) {
                                Text(
                                    text = "Cancel", 
                                    fontFamily = LeagueSpartan, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 18.sp,
                                    color = NavyBlue,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                            NavyPillButton("Delete", modifier = Modifier.weight(1f)) {
                                onDelete(c.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Walkie Talkie Icon matching reference UI design exactly ───────────────────
@Composable
fun WalkieTalkieIcon(modifier: Modifier = Modifier) {
    val navy = Color(0xFF0A1B4D)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Antenna (Offset to the left)
        drawRoundRect(
            color = navy,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.18f, 0f),
            size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f, w * 0.03f)
        )

        // 2. Main Walkie-Talkie Body (Capsule shape body with generous corners)
        drawRoundRect(
            color = navy,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.28f),
            size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f, w * 0.18f)
        )

        // 3. Speaker Grille Horizontal Lines (White/Silverish highlights)
        val lineW = w * 0.38f
        val lineH = h * 0.06f
        
        drawRoundRect(
            color = Color.White.copy(alpha = 0.95f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.31f, h * 0.48f),
            size = androidx.compose.ui.geometry.Size(lineW, lineH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
        )
        
        drawRoundRect(
            color = Color.White.copy(alpha = 0.95f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.31f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(lineW, lineH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
        )
    }
}

