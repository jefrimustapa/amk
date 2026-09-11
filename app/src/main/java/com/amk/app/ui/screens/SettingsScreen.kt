package com.amk.app.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amk.app.BuildConfig
import com.amk.app.hid.ConnectionState
import com.amk.app.hid.HidDeviceManager
import com.amk.app.model.AppSettings
import com.amk.app.model.ClickMode
import com.amk.app.ui.components.HapticFeedback
import com.amk.app.ui.theme.AccentCyan
import com.amk.app.ui.theme.AccentGreen
import com.amk.app.ui.theme.AccentRed
import com.amk.app.ui.theme.BorderStroke
import com.amk.app.ui.theme.DarkSurfaceVariant
import com.amk.app.ui.theme.TextMuted
import com.amk.app.ui.theme.TextPrimary
import com.amk.app.ui.theme.TextSecondary
import com.amk.app.update.UpdateInfo
import com.amk.app.update.UpdateService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    hidManager: HidDeviceManager,
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val connectionState by hidManager.connectionState.collectAsState()
    val connectedDeviceName by hidManager.connectedDeviceName.collectAsState()

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadStatusText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Trackpad Click Mode
        item {
            SectionCard(title = "Trackpad Click Behavior") {
                Text(
                    text = "Select how you want left & right clicks to be triggered on the trackpad:",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ClickModeSelector(
                    currentMode = settings.clickMode,
                    onModeSelected = { newMode ->
                        HapticFeedback.tick(context, settings.hapticEnabled)
                        onSettingsChanged(settings.copy(clickMode = newMode))
                    }
                )
            }
        }

        // Section: Pointer Physics & Scroll
        item {
            SectionCard(title = "Pointer & Physics") {
                Text(
                    text = "Pointer Speed: ${String.format("%.1fx", settings.pointerSpeed)}",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Slider(
                    value = settings.pointerSpeed,
                    onValueChange = { onSettingsChanged(settings.copy(pointerSpeed = it)) },
                    valueRange = 0.5f..3.0f,
                    steps = 25,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = BorderStroke
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingSwitchRow(
                    label = "Pointer Acceleration",
                    subtitle = "Dynamic speed scaling for swift pointer sweeps",
                    checked = settings.accelerationEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(accelerationEnabled = it)) }
                )

                SettingSwitchRow(
                    label = "Invert Two-Finger Scroll",
                    subtitle = "Natural vs traditional scroll direction",
                    checked = settings.invertScroll,
                    onCheckedChange = { onSettingsChanged(settings.copy(invertScroll = it)) }
                )

                SettingSwitchRow(
                    label = "Haptic Vibration",
                    subtitle = "Subtle tactile click feedback on tap & buttons",
                    checked = settings.hapticEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(hapticEnabled = it)) }
                )
            }
        }

        // Section: Bluetooth Pairing & Discovery
        item {
            SectionCard(title = "Bluetooth Device Management") {
                Text(
                    text = "Status: ${if (connectionState == ConnectionState.CONNECTED) "Connected to $connectedDeviceName" else "Disconnected"}",
                    color = if (connectionState == ConnectionState.CONNECTED) AccentGreen else TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionBtn(
                        label = "Pair New TV",
                        icon = Icons.Default.BluetoothSearching,
                        modifier = Modifier.weight(1f)
                    ) {
                        try {
                            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(discoverableIntent)
                            Toast.makeText(context, "Phone is now discoverable on TV for 3 minutes", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error enabling discoverable mode", Toast.LENGTH_SHORT).show()
                        }
                    }

                    if (connectionState == ConnectionState.CONNECTED) {
                        ActionBtn(
                            label = "Disconnect",
                            icon = Icons.Default.Bluetooth,
                            modifier = Modifier.weight(1f)
                        ) {
                            hidManager.disconnect()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Paired Devices (Tap to connect):",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val paired = hidManager.getPairedDevices()
                if (paired.isEmpty()) {
                    Text(
                        text = "No paired devices found. Put TV in 'Add Accessory' mode.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    paired.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .clickable { hidManager.connect(device) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = device.name ?: "Unknown TV/Device", color = TextPrimary, fontSize = 14.sp)
                                Text(text = device.address, color = TextMuted, fontSize = 11.sp)
                            }
                            Text(text = "Connect", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Persistent Storage Info
        item {
            SectionCard(title = "Persistent Storage (Zero Data Loss)") {
                Text(
                    text = "Settings automatically mirror to public storage:\n/sdcard/Documents/AMK/amk_settings.json",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✅ Your configurations remain preserved and automatically restore if you uninstall and reinstall the application.",
                    color = AccentGreen,
                    fontSize = 12.sp
                )
            }
        }

        // Section: OTA App Updates
        item {
            SectionCard(title = "Application Updates (OTA)") {
                Text(
                    text = "Installed Version: v${BuildConfig.APP_VERSION_NAME}",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = "Build Channel: ${BuildConfig.APP_CHANNEL} • #${BuildConfig.APP_BUILD_NUMBER}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SettingSwitchRow(
                    label = "Include Nightly Builds",
                    subtitle = "Get automated bleeding-edge updates daily",
                    checked = settings.includeNightly,
                    onCheckedChange = { onSettingsChanged(settings.copy(includeNightly = it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ActionBtn(
                    label = if (isCheckingUpdate) "Checking GitHub..." else "Check for Updates",
                    icon = Icons.Default.CloudDownload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    coroutineScope.launch {
                        isCheckingUpdate = true
                        updateResult = UpdateService.checkForUpdates(settings.includeNightly)
                        isCheckingUpdate = false
                    }
                }

                updateResult?.let { info ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, BorderStroke, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = if (info.hasUpdate) "🎉 Update Available: v${info.latestVersion}" else "✅ You're up to date!",
                                color = if (info.hasUpdate) AccentCyan else AccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = info.releaseNotes,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            if (info.hasUpdate && info.apkUrl != null) {
                                if (downloadProgress in 1..99) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = AccentCyan
                                    )
                                    Text(
                                        text = downloadStatusText ?: "$downloadProgress%",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else {
                                    ActionBtn(
                                        label = "Download & Install (${info.apkSizeMb ?: "APK"})",
                                        icon = Icons.Default.CloudDownload,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        coroutineScope.launch {
                                            UpdateService.downloadAndInstallApk(
                                                context = context,
                                                downloadUrl = info.apkUrl,
                                                apkFileName = info.apkName ?: "amk-update.apk",
                                                onProgress = { p, status ->
                                                    downloadProgress = p
                                                    downloadStatusText = status
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClickModeSelector(
    currentMode: ClickMode,
    onModeSelected: (ClickMode) -> Unit
) {
    val options = listOf(
        ClickMode.TAP_ONLY to "Tap Gestures Only",
        ClickMode.BUTTONS_ONLY to "Physical Buttons Only",
        ClickMode.BOTH to "Both Gestures & Buttons"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (mode, label) ->
            val selected = currentMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) AccentCyan.copy(alpha = 0.15f) else DarkSurfaceVariant)
                    .border(1.dp, if (selected) AccentCyan else BorderStroke, RoundedCornerShape(12.dp))
                    .clickable { onModeSelected(mode) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    color = if (selected) AccentCyan else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                if (selected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, BorderStroke, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextPrimary, fontSize = 14.sp)
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.Black,
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
fun ActionBtn(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, BorderStroke, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = AccentCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
