package com.amk.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amk.app.hid.HidConstants
import com.amk.app.hid.HidDeviceManager
import com.amk.app.model.AppSettings
import com.amk.app.ui.components.HapticFeedback
import com.amk.app.ui.theme.AccentCyan
import com.amk.app.ui.theme.AccentRed
import com.amk.app.ui.theme.BorderStroke
import com.amk.app.ui.theme.DarkSurfaceVariant
import com.amk.app.ui.theme.TextPrimary
import com.amk.app.ui.theme.TextSecondary

@Composable
fun RemoteScreen(
    hidManager: HidDeviceManager,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun sendConsumer(code: Short) {
        HapticFeedback.tick(context, settings.hapticEnabled)
        hidManager.sendConsumerKey(code)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top System Controls: Power, Home, Back, Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RoundButton(icon = Icons.Default.PowerSettingsNew, tint = AccentRed) {
                sendConsumer(HidConstants.CONSUMER_POWER)
            }
            RoundButton(icon = Icons.Default.Home) {
                sendConsumer(HidConstants.CONSUMER_HOME)
            }
            RoundButton(icon = Icons.Default.ArrowBack) {
                sendConsumer(HidConstants.CONSUMER_BACK)
            }
            RoundButton(icon = Icons.Default.Menu) {
                sendConsumer(HidConstants.CONSUMER_MENU)
            }
        }

        // 5-Way Ergonomic D-Pad
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .border(2.dp, BorderStroke, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // D-Pad Up
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .size(60.dp)
                    .clickable { sendConsumer(HidConstants.CONSUMER_DPAD_UP) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }

            // D-Pad Down
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .size(60.dp)
                    .clickable { sendConsumer(HidConstants.CONSUMER_DPAD_DOWN) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }

            // D-Pad Left
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(60.dp)
                    .clickable { sendConsumer(HidConstants.CONSUMER_DPAD_LEFT) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }

            // D-Pad Right
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(60.dp)
                    .clickable { sendConsumer(HidConstants.CONSUMER_DPAD_RIGHT) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = TextPrimary, modifier = Modifier.size(36.dp))
            }

            // Center OK Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AccentCyan)
                    .clickable { sendConsumer(HidConstants.CONSUMER_DPAD_SELECT) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OK",
                    color = androidx.compose.ui.graphics.Color.Black,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }

        // Media & Volume Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Volume Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaButton(
                    label = "Vol -",
                    icon = Icons.Default.VolumeDown,
                    modifier = Modifier.weight(1f)
                ) {
                    sendConsumer(HidConstants.CONSUMER_VOLUME_DOWN)
                }
                MediaButton(
                    label = "Mute",
                    icon = Icons.Default.VolumeMute,
                    modifier = Modifier.weight(1f)
                ) {
                    sendConsumer(HidConstants.CONSUMER_MUTE)
                }
                MediaButton(
                    label = "Vol +",
                    icon = Icons.Default.VolumeUp,
                    modifier = Modifier.weight(1f)
                ) {
                    sendConsumer(HidConstants.CONSUMER_VOLUME_UP)
                }
            }

            // Playback Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaButton(
                    label = "Rewind",
                    icon = Icons.Default.FastRewind,
                    modifier = Modifier.weight(1f)
                ) {
                    sendConsumer(HidConstants.CONSUMER_SCAN_PREV)
                }
                MediaButton(
                    label = "Play / Pause",
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f)
                ) {
                    sendConsumer(HidConstants.CONSUMER_PLAY_PAUSE)
                }
                MediaButton(
                    label = "Forward",
                    icon = Icons.Default.FastForward,
                    modifier = Modifier.weight(1f)
                ) {
                    sendConsumer(HidConstants.CONSUMER_SCAN_NEXT)
                }
            }
        }
    }
}

@Composable
fun RoundButton(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(DarkSurfaceVariant)
            .border(1.dp, BorderStroke, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun MediaButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, color = TextPrimary, fontSize = 13.sp)
        }
    }
}
