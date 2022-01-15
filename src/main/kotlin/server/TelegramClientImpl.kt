package server

import data.telegram.TelegramBotInfo
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.utils.PreviewFeature
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramClientImpl(
    private val telegramBotInfo: TelegramBotInfo,
) : Client, CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        SupervisorJob() + Dispatchers.IO
    }

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState>
        get() = _state

    private var botBehaviour: BehaviourContext? = null

    override fun send(message: String) {
        val client = botBehaviour ?: return
        launch {
            val chatId = if (telegramBotInfo.channelId.startsWith("-100"))
                telegramBotInfo.channelId.toLongOrNull()
            else
                "-100${telegramBotInfo.channelId}".toLongOrNull()
            if (chatId == null) {
                return@launch
            }
            client.sendTextMessage(ChatId(chatId), message)
        }
    }

    @OptIn(PreviewFeature::class)
    override fun start(): Flow<String> {
        return channelFlow {
            _state.value = ConnectionState.Connecting
            telegramBotWithBehaviourAndLongPolling(telegramBotInfo.token, this@TelegramClientImpl) {
                val self = getMe()
                botBehaviour = this
                _state.value = ConnectionState.Connected
                onContentMessage { message ->
                    if (self.id == message.asFromUser()?.from?.id) {
                        return@onContentMessage
                    }
                    message.content.asTextContent()?.text?.let { send(it) }
                }
            }.second.join()
        }
            .onCompletion {
                botBehaviour?.scope?.cancel()
                botBehaviour = null
                _state.value = ConnectionState.Disconnected
            }
    }
}
