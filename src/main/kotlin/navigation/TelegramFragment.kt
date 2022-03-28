package navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.telegram.TelegramBotInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.koin.core.component.inject
import server.ConnectionState
import ui.Fragment
import ui.TelegramViewModel
import ui.common.ChooserTitle
import ui.common.ConnectionStatus
import ui.common.PasswordVisibilityIndicator
import utils.AppColor
import utils.modifyIf

class TelegramFragment : Fragment() {

    private var telegramBotInfo by mutableStateOf(TelegramBotInfo())

    private val viewModel by inject<TelegramViewModel>()

    private var telegramConnectionState by mutableStateOf<ConnectionState>(ConnectionState.Disconnected)

    @Composable
    private fun TelegramLogo() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(AppColor.Telegram)
        ) {
            Image(
                bitmap = useResource("telegram_logo.png") { loadImageBitmap(it) },
                contentDescription = "Telegram Logo",
            )
        }
    }

    @Composable
    private fun ConnectionPanel() {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            ConnectionStatus(
                telegramConnectionState,
                modifier = Modifier
                    .align(Alignment.CenterVertically),
            )
            Button(
                onClick = {
                    println(telegramConnectionState.toString())
                    if (telegramConnectionState is ConnectionState.Disconnected) {
                        viewModel.connectTelegram()
                    } else {
                        viewModel.disconnect()
                    }
                },
            ) {
                Text(
                    text = if (telegramConnectionState is ConnectionState.Disconnected) "Start" else "Stop",
                )
            }
        }
    }

    @Composable
    private fun TelegramPanel() {
        Column {
            TelegramLogo()
            ConnectionPanel()
            TokenChooser()
            ChannelChooser()
        }
    }

    @Composable
    private fun TokenChooser() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            var showToken by remember { mutableStateOf(false) }
            TextField(
                value = telegramBotInfo.token,
                onValueChange = {
                    telegramBotInfo = telegramBotInfo.copy(token = it)
                    viewModel.updateToken(it)
                },
                label = { Text("Token") },
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .weight(1f),
            )
            PasswordVisibilityIndicator(
                showToken,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 4.dp)
                    .clickable { showToken = !showToken }
            )
        }
    }

    private var channelUpdateJob: Job? = null

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ChannelChooser() {
        var updating by remember { mutableStateOf(false) }
        val animateTween by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Restart)
        )
        Column {
            Row(
                modifier = Modifier
                    .padding(8.dp)
            ) {
                ChooserTitle(title = "Channel")
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Update channel",
                    tint = AppColor.Primary,
                    modifier = Modifier
                        .pointerHoverIcon(if (updating) PointerIconDefaults.Default else PointerIconDefaults.Hand)
                        .clickable(!updating) {
                            if (telegramConnectionState !is ConnectionState.Connected) {
                                println("Telegram not connected")
                                return@clickable
                            }
                            channelUpdateJob?.cancel()
                            channelUpdateJob = viewModel.waitForChannel()
                                .onStart { updating = true }
                                .onCompletion { updating = false }
                                .launchIn(lifecycleScope)
                        }
                        .align(Alignment.CenterVertically)
                        .modifyIf(updating) { rotate(animateTween) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (telegramBotInfo.channelId.isEmpty()) {
                    Text(
                        text = "Channel not chosen",
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 8.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .background(AppColor.DarkBackground, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = telegramBotInfo.channelTitle,
                            fontWeight = FontWeight.Bold,
                            color = AppColor.White,
                            modifier = Modifier
                                .padding(4.dp)
                        )
                    }
                }
            }
            if (updating) {
                Text(
                    text = "Pin any message in the Telegram channel you want to bind.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                )
            }
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(this) {
            viewModel.telegramConnectionState
                .onEach { telegramConnectionState = it }
                .launchIn(lifecycleScope)
            viewModel.botInfoFlow
                .onEach { info ->
                    println(info.toString())
                    telegramBotInfo = info
                }
                .launchIn(lifecycleScope)
        }
        TelegramPanel()
    }
}
