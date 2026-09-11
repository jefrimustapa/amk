package com.amk.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amk.app.hid.HidConstants
import com.amk.app.hid.HidDeviceManager
import com.amk.app.hid.KeycodeMapper
import com.amk.app.model.AppSettings
import com.amk.app.ui.components.HapticFeedback
import com.amk.app.ui.theme.AccentCyan
import com.amk.app.ui.theme.BorderStroke
import com.amk.app.ui.theme.DarkSurfaceVariant
import com.amk.app.ui.theme.TextMuted
import com.amk.app.ui.theme.TextPrimary
import com.amk.app.ui.theme.TextSecondary

@Composable
fun KeyboardScreen(
    hidManager: HidDeviceManager,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    fun sendChar(c: Char) {
        val mapping = KeycodeMapper.charToHid(c)
        if (mapping != null) {
            HapticFeedback.tick(context, settings.hapticEnabled)
            hidManager.sendKeyboardReport(mapping.first, mapping.second)
        }
    }

    fun sendKey(keycode: Byte, modifierByte: Byte = HidConstants.MOD_NONE) {
        HapticFeedback.tick(context, settings.hapticEnabled)
        hidManager.sendKeyboardReport(modifierByte, keycode)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Live Keyboard Forwarder",
            color = TextPrimary,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Type text here to stream characters straight to the TV",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Live text input box
        OutlinedTextField(
            value = inputText,
            onValueChange = { newText ->
                if (newText.length > inputText.length) {
                    // Character(s) added
                    val added = newText.substring(inputText.length)
                    for (c in added) {
                        sendChar(c)
                    }
                } else if (newText.length < inputText.length) {
                    // Backspace triggered
                    val count = inputText.length - newText.length
                    for (i in 0 until count) {
                        sendKey(HidConstants.KEY_BACKSPACE)
                    }
                }
                inputText = newText
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            placeholder = { Text("Tap to activate keyboard...", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = BorderStroke,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    sendKey(HidConstants.KEY_ENTER)
                }
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick TV Hotkey Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KeyButton(
                label = "Enter",
                icon = Icons.Default.KeyboardReturn,
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_ENTER)
            }
            KeyButton(
                label = "Backspace",
                icon = Icons.Default.Backspace,
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_BACKSPACE)
            }
            KeyButton(
                label = "Esc / Back",
                icon = Icons.Default.ArrowBack,
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_ESCAPE)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick TV Hotkey Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KeyButton(
                label = "Space",
                icon = Icons.Default.SpaceBar,
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_SPACE)
            }
            KeyButton(
                label = "Tab",
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_TAB)
            }
            KeyButton(
                label = "Paste",
                icon = Icons.Default.ContentPaste,
                modifier = Modifier.weight(1f)
            ) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                for (c in clipText) {
                    sendChar(c)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Arrow navigation bar
        Text(
            text = "Cursor Navigation",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyButton(
                label = "Up",
                icon = Icons.Default.ArrowUpward,
                modifier = Modifier.weight(0.5f)
            ) {
                sendKey(HidConstants.KEY_UP)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KeyButton(
                label = "Left",
                icon = Icons.Default.ArrowBack,
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_LEFT)
            }
            KeyButton(
                label = "Down",
                icon = Icons.Default.ArrowDownward,
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_DOWN)
            }
            KeyButton(
                label = "Right",
                icon = Icons.Default.ArrowForward,
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_RIGHT)
            }
        }
    }
}

@Composable
fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
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
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TextPrimary,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 13.sp
            )
        }
    }
}
