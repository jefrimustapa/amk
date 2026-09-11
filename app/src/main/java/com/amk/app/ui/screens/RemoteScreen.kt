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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.HorizontalDivider
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
import com.amk.app.ui.theme.DarkSurface
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
            RoundButton(icon = Icons.Rounded.PowerSettingsNew, tint = AccentRed) {
                sendConsumer(HidConstants.CONSUMER_POWER)
            }
            RoundButton(icon = Icons.Rounded.Home) {
                sendConsumer(HidConstants.CONSUMER_HOME)
            }
            RoundButton(icon = Icons.Rounded.ArrowBack) {
                sendConsumer(HidConstants.CONSUMER_BACK)
            }
            RoundButton(icon = Icons.Rounded.Menu) {
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
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Up", tint = TextPrimary, modifier = Modifier.size(36.dp))
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
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Down", tint = TextPrimary, modifier = Modifier.size(36.dp))
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
                Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "Left", tint = TextPrimary, modifier = Modifier.size(36.dp))
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
                Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Right", tint = TextPrimary, modifier = Modifier.size(36.dp))
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
                    icon = Icons.Rounded.VolumeDown,
                    modifier = Modifier.weight(1f)
                ) {
                    sendConsumer(HidConstants.CONSUMER_VOLUME_DOWN)
                }
                MediaButton(
                    label = "Mute",
                    icon = Icons.Rounded.VolumeMute,
                    modifier = Modifier.weight(1f)
                ) {
                    sendConsumer(HidConstants.CONSUMER_MUTE)
                }
                MediaButton(
                    label = "Vol +",
                    icon = Icons.Rounded.VolumeUp,
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
                    icon = Icons.Rounded.FastRewind,
                    modifier = Modifier.weight(1f)
                ) {
                    sendConsumer(HidConstants.CONSUMER_SCAN_PREV)
                }
                MediaButton(
                    label = "Play / Pause",
                    icon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.weight(1f)
                ) {
                    HapticFeedback.tick(context, settings.hapticEnabled)
                    hidManager.sendPlayPauseCombo()
                }
                MediaButton(
                    label = "Forward",
                    icon = Icons.Rounded.FastForward,
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
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top half: Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TextPrimary,
                    modifier = Modifier.height(20.dp)
                )
            }

            // Shade Divider
            HorizontalDivider(
                thickness = 1.dp,
                color = BorderStroke.copy(alpha = 0.7f)
            )

            // Bottom half: Description with shaded background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .background(DarkSurface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
