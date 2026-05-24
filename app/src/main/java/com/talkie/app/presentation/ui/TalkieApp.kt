package com.talkie.app.presentation.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talkie.app.domain.Contact
import com.talkie.app.presentation.TalkieViewModel

private sealed class Screen {
    object Home : Screen()
    data class Ptt(val contact: Contact) : Screen()
    data class FriendProfile(val contact: Contact) : Screen()
    object OwnProfile : Screen()
}

private enum class Overlay { NONE, SETTINGS, ADD, EDIT, DELETE }

@Composable
fun TalkieApp(viewModel: TalkieViewModel, onLogout: () -> Unit = {}) {
    // ── Collect state ─────────────────────────────────────────────────────────
    val contacts       by viewModel.contacts.collectAsStateWithLifecycle()
    val isTransmitting by viewModel.isTransmitting.collectAsStateWithLifecycle()
    val isReceiving    by viewModel.isReceiving.collectAsStateWithLifecycle()
    val connectedPeer  by viewModel.connectedPeer.collectAsStateWithLifecycle()
    val deviceName     by viewModel.deviceName.collectAsStateWithLifecycle()
    val devicePhoto    by viewModel.devicePhotoUri.collectAsStateWithLifecycle()
    val deviceBio      by viewModel.deviceBio.collectAsStateWithLifecycle()
    val localIp        by viewModel.localIpAddress.collectAsStateWithLifecycle()
    val debugInfo      by viewModel.debugInfo.collectAsStateWithLifecycle()
    val incomingAmplitude by viewModel.incomingAmplitude.collectAsStateWithLifecycle()

    // ── Navigation & overlay state ────────────────────────────────────────────
    var screen  by remember { mutableStateOf<Screen>(Screen.Home) }
    var overlay by remember { mutableStateOf(Overlay.NONE) }

    // Which contact is currently "speaking" (isReceiving + we know who's connected)
    val receivingFromId = if (isReceiving) connectedPeer?.deviceAddress?.let { ip ->
        contacts.firstOrNull { it.ipAddress == ip }?.id
    } else null

    // ── Permissions ───────────────────────────────────────────────────────────
    val context = LocalContext.current
    val permissions = remember {
        mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO)
            .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS) }
    }
    var granted by remember {
        mutableStateOf(permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        })
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = it.values.all { v -> v }
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(permissions.toTypedArray())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }
    if (!granted) { PermissionScreen(); return }

    // ── Screen routing ────────────────────────────────────────────────────────
    // ── Screen routing ────────────────────────────────────────────────────────
    when (val s = screen) {
        is Screen.Home -> HomeScreen(
            deviceName        = deviceName,
            devicePhotoUri    = devicePhoto,
            contacts          = contacts,
            receivingFrom     = receivingFromId,
            incomingAmplitude = incomingAmplitude,
            onAvatarClick     = { screen = Screen.OwnProfile },
            onMenuClick       = { overlay = Overlay.SETTINGS },
            onContactAvatar   = { screen = Screen.FriendProfile(it) },
            onTalk = { contact ->
                viewModel.connectToIp(contact.ipAddress, contact.name)
                screen = Screen.Ptt(contact)
            },
            onSync = { contact -> viewModel.connectToIp(contact.ipAddress, contact.name) }
        )
        is Screen.Ptt -> PttScreen(
            contact        = s.contact,
            isTransmitting = isTransmitting,
            onPress        = { viewModel.startTransmitting() },
            onRelease      = { viewModel.stopTransmitting() },
            onMenuClick    = { viewModel.disconnect(); screen = Screen.Home }
        )
        is Screen.OwnProfile -> ProfileScreen(
            name         = deviceName,
            bio          = deviceBio,
            photoUri     = devicePhoto,
            ipAddress    = localIp,
            isOwnProfile = true,
            onBack       = { screen = Screen.Home },
            onNameSave   = { viewModel.updateDeviceName(it) },
            onPhotoSave  = { viewModel.updateDevicePhoto(it) }
        )
        is Screen.FriendProfile -> ProfileScreen(
            name         = s.contact.name,
            bio          = s.contact.bio,
            photoUri     = s.contact.photoUri,
            ipAddress    = s.contact.ipAddress,
            isOwnProfile = false,
            onBack       = { screen = Screen.Home },
            onTalk       = {
                viewModel.connectToIp(s.contact.ipAddress, s.contact.name)
                screen = Screen.Ptt(s.contact)
            }
        )
    }

    // ── Overlays (rendered on top of whatever screen is showing) ─────────────
    when (overlay) {
        Overlay.SETTINGS -> SettingsSheet(
            onAddContact    = { overlay = Overlay.ADD },
            onEditContact   = { overlay = Overlay.EDIT },
            onDeleteContact = { overlay = Overlay.DELETE },
            onLogout        = { overlay = Overlay.NONE; onLogout() },
            onDismiss       = { overlay = Overlay.NONE }
        )
        Overlay.ADD -> AddContactDialog(
            onSave    = { name, ip, uri -> viewModel.addContact(name, ip, uri); overlay = Overlay.NONE },
            onDismiss = { overlay = Overlay.NONE }
        )
        Overlay.EDIT -> EditContactDialog(
            contacts  = contacts,
            onSave    = { id, name, ip, uri -> viewModel.updateContact(id, name, ip, uri); overlay = Overlay.NONE },
            onDismiss = { overlay = Overlay.NONE }
        )
        Overlay.DELETE -> DeleteContactDialog(
            contacts  = contacts,
            onDelete  = { id -> viewModel.deleteContact(id); overlay = Overlay.NONE },
            onDismiss = { overlay = Overlay.NONE }
        )
        Overlay.NONE -> Unit
    }
}

@Composable
private fun PermissionScreen() {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Mic, contentDescription = null,
                tint = NavyBlue, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Permissions Required", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NavyBlue)
            Spacer(Modifier.height(8.dp))
            Text("Talkie needs microphone and location\npermissions to work.",
                color = Color.Gray, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}
