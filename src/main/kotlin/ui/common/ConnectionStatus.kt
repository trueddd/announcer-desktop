package ui.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import server.ConnectionState

@Preview
@Composable
fun ConnectionStatus(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    when (connectionState) {
                        is ConnectionState.Disconnected -> Color.Red
                        is ConnectionState.Connected -> Color.Green
                        is ConnectionState.Connecting -> Color.Yellow
                    },
                    CircleShape
                )
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = when (connectionState) {
                ConnectionState.Connected -> "Connected"
                ConnectionState.Connecting -> "Connecting..."
                ConnectionState.Disconnected -> "Disconnected"
            },
        )
    }
}
