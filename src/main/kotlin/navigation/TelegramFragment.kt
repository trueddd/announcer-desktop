package navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import data.telegram.TelegramBotInfo
import db.TelegramRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import server.Client
import server.ConnectionState
import ui.Fragment
import ui.common.ConnectionStatus
import ui.common.PasswordVisibilityIndicator
import utils.AppColor

class TelegramFragment(
    private val commonMessagesFlow: MutableSharedFlow<Pair<String, String>>,
) : Fragment() {

    private val telegramRepository by inject<TelegramRepository>()
    private var telegramBotInfo by mutableStateOf(TelegramBotInfo())
    private var telegramConnectionState by mutableStateOf<ConnectionState>(ConnectionState.Disconnected)
    private var telegramJob: Job? = null

    private fun connectTelegram() {
        val client = get<Client>(named(Client.Type.Telegram)) { parametersOf(telegramBotInfo) }
        telegramJob = lifecycleScope.launch {
            client.start()
                .onEach {
                    commonMessagesFlow.tryEmit(Client.Type.Telegram to it)
                }
                .launchIn(this)
            client.state
                .onEach { telegramConnectionState = it }
                .onCompletion { telegramConnectionState = ConnectionState.Disconnected }
                .launchIn(this)
            commonMessagesFlow
                .transform { (client, message) ->
                    if (client != Client.Type.Telegram) {
                        emit(message)
                    }
                }
                .onEach { client.send(it) }
                .launchIn(this)
        }
    }

    private fun onTelegramButtonClicked() {
        when {
            telegramJob?.isActive == true -> {
                telegramJob?.cancel()
                telegramJob = null
            }
            telegramBotInfo.isValid -> {
                connectTelegram()
            }
            else -> {
                println("tg: invalid")
            }
        }
    }

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
                onClick = ::onTelegramButtonClicked,
            ) {
                Text(
                    text = if (telegramConnectionState is ConnectionState.Disconnected) "Start" else "Stop",
                )
            }
        }
    }

    private val chatIdRegex = Regex(".*web\\.telegram\\.org/\\?legacy=1#/im\\?p=c(\\d{10})_.*")
    private fun String.retrieveTelegramChatId(): String {
        return chatIdRegex.find(this)?.groupValues
            ?.let { if (it.size == 2) it[1] else null } ?: this
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
                    lifecycleScope.launch { telegramRepository.updateToken(it) }
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
        TextField(
            value = telegramBotInfo.channelId,
            onValueChange = {
                val newId = it.retrieveTelegramChatId()
                telegramBotInfo = telegramBotInfo.copy(channelId = newId)
                lifecycleScope.launch { telegramRepository.updateChannel(newId) }
            },
            singleLine = true,
            label = { Text("Channel ID") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth(),
        )
    }

    @Composable
    override fun Content() {
        LaunchedEffect(this) {
            telegramRepository.getTelegramBotInfoFlow()
                .withIndex()
                .onEach { (index, info) ->
                    println(info.toString())
                    telegramBotInfo = info
                    if (index == 0 && info.isValid) {
                        connectTelegram()
                    }
                }
                .launchIn(lifecycleScope)
        }
        TelegramPanel()
    }
}
