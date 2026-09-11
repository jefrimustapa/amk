package com.amk.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amk.app.hid.ConnectionState
import com.amk.app.ui.theme.AccentCyan
import com.amk.app.ui.theme.AccentGreen
import com.amk.app.ui.theme.AccentRed
import com.amk.app.ui.theme.BorderStroke
import com.amk.app.ui.theme.DarkSurfaceVariant
import com.amk.app.ui.theme.TextPrimary
import com.amk.app.ui.theme.TextSecondary

@Composable
fun ConnectionBadge(
    connectionState: ConnectionState,
    deviceName: String?,
    modifier: Modifier = Modifier
) {
    val indicatorColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CONNECTED -> AccentGreen
            ConnectionState.CONNECTING -> AccentCyan
            ConnectionState.DISCONNECTED -> AccentRed
        },
        label = "indicatorColor"
    )

    val labelText = when (connectionState) {
        ConnectionState.CONNECTED -> deviceName ?: "Connected"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.DISCONNECTED -> "Disconnected"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = labelText,
            color = if (connectionState == ConnectionState.CONNECTED) TextPrimary else TextSecondary,
            fontSize = 12.sp
        )
    }
}
