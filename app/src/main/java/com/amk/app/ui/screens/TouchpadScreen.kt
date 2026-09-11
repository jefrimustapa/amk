package com.amk.app.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amk.app.hid.HidDeviceManager
import com.amk.app.model.AppSettings
import com.amk.app.model.ClickMode
import com.amk.app.ui.components.HapticFeedback
import com.amk.app.ui.theme.AccentBlue
import com.amk.app.ui.theme.AccentCyan
import com.amk.app.ui.theme.BorderStroke
import com.amk.app.ui.theme.DarkSurface
import com.amk.app.ui.theme.DarkSurfaceVariant
import com.amk.app.ui.theme.TextMuted
import com.amk.app.ui.theme.TextPrimary
import com.amk.app.ui.theme.TextSecondary
import com.amk.app.ui.theme.TrackpadBorder
import com.amk.app.ui.theme.TrackpadSurface
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchpadScreen(
    hidManager: HidDeviceManager,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Touchpad state
    var lastX by remember { mutableFloatStateOf(0f) }
    var lastY by remember { mutableFloatStateOf(0f) }
    var touchDownTime by remember { mutableLongStateOf(0L) }
    var pointerCount by remember { mutableStateOf(1) }
    var hasMovedSignificantly by remember { mutableStateOf(false) }

    val showButtons = settings.clickMode == ClickMode.BUTTONS_ONLY || settings.clickMode == ClickMode.BOTH
    val tapEnabled = settings.clickMode == ClickMode.TAP_ONLY || settings.clickMode == ClickMode.BOTH

    fun applySpeed(delta: Float): Int {
        val speed = delta * settings.pointerSpeed
        return if (settings.accelerationEnabled) {
            val accelerated = speed.sign * (abs(speed).pow(1.15f))
            accelerated.toInt()
        } else {
            speed.toInt()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Trackpad Surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(DarkSurfaceVariant.copy(alpha = 0.5f), TrackpadSurface),
                        radius = 800f
                    )
                )
                .border(1.5.dp, TrackpadBorder, RoundedCornerShape(24.dp))
                .pointerInteropFilter { motionEvent ->
                    pointerCount = motionEvent.pointerCount

                    when (motionEvent.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            lastX = motionEvent.x
                            lastY = motionEvent.y
                            touchDownTime = System.currentTimeMillis()
                            hasMovedSignificantly = false
                            true
                        }

                        MotionEvent.ACTION_POINTER_DOWN -> {
                            // Secondary finger touched down
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (motionEvent.pointerCount == 1) {
                                val dx = motionEvent.x - lastX
                                val dy = motionEvent.y - lastY

                                if (abs(dx) > 3 || abs(dy) > 3) {
                                    hasMovedSignificantly = true
                                    val sendDx = applySpeed(dx)
                                    val sendDy = applySpeed(dy)
                                    hidManager.sendMouseReport(0, sendDx, sendDy, 0)
                                }
                                lastX = motionEvent.x
                                lastY = motionEvent.y
                            } else if (motionEvent.pointerCount == 2) {
                                // Two-finger vertical scroll
                                val currentY = (motionEvent.getY(0) + motionEvent.getY(1)) / 2f
                                val dy = currentY - lastY

                                if (abs(dy) > 5) {
                                    hasMovedSignificantly = true
                                    val scrollFactor = if (settings.invertScroll) 1 else -1
                                    val wheel = (dy * 0.2f * scrollFactor).toInt().coerceIn(-127, 127)
                                    if (wheel != 0) {
                                        hidManager.sendMouseReport(0, 0, 0, wheel)
                                    }
                                }
                                lastY = currentY
                            }
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            val elapsed = System.currentTimeMillis() - touchDownTime
                            if (!hasMovedSignificantly && elapsed < 350 && tapEnabled) {
                                // Single tap = Left Click
                                HapticFeedback.tick(context, settings.hapticEnabled)
                                hidManager.sendMouseReport(1, 0, 0, 0)
                                hidManager.sendMouseReport(0, 0, 0, 0)
                            }
                            true
                        }

                        MotionEvent.ACTION_POINTER_UP -> {
                            val elapsed = System.currentTimeMillis() - touchDownTime
                            if (!hasMovedSignificantly && elapsed < 350 && tapEnabled && pointerCount >= 2) {
                                // Two-finger tap = Right Click
                                HapticFeedback.tick(context, settings.hapticEnabled)
                                hidManager.sendMouseReport(2, 0, 0, 0)
                                hidManager.sendMouseReport(0, 0, 0, 0)
                            }
                            true
                        }

                        else -> false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PanTool,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Trackpad Surface",
                    color = TextMuted.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Text(
                    text = if (tapEnabled) "1-finger tap: Left click • 2-finger: Right click & scroll" else "Gliding only (Buttons below)",
                    color = TextMuted.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Dedicated Bottom Left & Right Click Buttons
        if (showButtons) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Click Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, BorderStroke, RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            HapticFeedback.tick(context, settings.hapticEnabled)
                            hidManager.sendMouseReport(1, 0, 0, 0)
                            hidManager.sendMouseReport(0, 0, 0, 0)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Left Click",
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }

                // Right Click Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, BorderStroke, RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            HapticFeedback.tick(context, settings.hapticEnabled)
                            hidManager.sendMouseReport(2, 0, 0, 0)
                            hidManager.sendMouseReport(0, 0, 0, 0)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Right Click",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
