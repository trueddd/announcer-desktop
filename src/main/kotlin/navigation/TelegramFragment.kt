package navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import data.telegram.TelegramBotInfo
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject
import server.ConnectionState
import ui.Fragment
import ui.TelegramViewModel
import ui.common.ConnectionStatus
import ui.common.PasswordVisibilityIndicator
import utils.AppColor

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
        TelegramLogo()
        ConnectionPanel()
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
        Text(
            text = telegramBotInfo.channelTitle.ifEmpty { "Channel not chosen" },
        )
        Button(
            onClick = {
                viewModel.waitForChannel()
                    .launchIn(lifecycleScope)
            },
        ) {
            Text(text = "Update telegram chat")
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
