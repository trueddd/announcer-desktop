package ui

import data.telegram.TelegramBotInfo
import db.TelegramRepository
import dev.inmo.tgbotapi.types.chat.abstracts.ChannelChat
import di.MessagesFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import server.Client
import server.ConnectionState
import server.TelegramClient

class TelegramViewModel(
    private val telegramRepository: TelegramRepository,
    private val telegramClient: TelegramClient,
    private val messagesFlow: MessagesFlow,
) : BaseViewModel() {

    private var telegramJob: Job? = null

    val telegramConnectionState = telegramClient.state
        .stateIn(this, SharingStarted.Eagerly, ConnectionState.Disconnected)

    val botInfoFlow = telegramRepository.getTelegramBotInfoFlow()
        .stateIn(this, SharingStarted.Eagerly, TelegramBotInfo())

    fun updateToken(newToken: String) = launch { telegramRepository.updateToken(newToken) }

    fun waitForChannel(): Flow<ChannelChat> {
        return telegramClient.waitForChatWithPinnedMessage()
            .onStart { println("Updating chat") }
            .onEach { telegramRepository.updateChat(it) }
            .onCompletion {
                println("Finished telegram update")
                println(telegramRepository.getTelegramBotInfoFlow().first())
            }
    }

    fun disconnect() {
        if (telegramJob?.isActive == true) {
            telegramJob?.cancel()
            telegramJob = null
        }
    }

    fun connectTelegram() {
        if (!botInfoFlow.value.canConnect) {
            println("Info not valid")
            return
        }
        telegramJob = launch {
            telegramClient.start()
                .onEach { messagesFlow.tryEmit(Client.Type.Telegram to it) }
                .launchIn(this)
            messagesFlow
                .transform { (client, message) ->
                    if (client != Client.Type.Telegram) {
                        emit(message)
                    }
                }
                .onEach { telegramClient.send(it) }
                .launchIn(this)
        }
    }
}