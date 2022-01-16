package server

import data.telegram.TelegramBotInfo
import db.TelegramRepository
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
    private val telegramRepository: TelegramRepository,
) : Client, CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        SupervisorJob() + Dispatchers.IO
    }

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState>
        get() = _state

    private var botBehaviour: BehaviourContext? = null

    private var chatId = telegramBotInfo.channelId.toChatId()

    private fun String.toChatId(): Long? {
        return if (startsWith("-100"))
            toLongOrNull()
        else
            "-100${this}".toLongOrNull()
    }

    override fun send(message: String) {
        val client = botBehaviour ?: return
        launch {
            chatId?.let { client.sendTextMessage(ChatId(it), message) }
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
                telegramRepository.getTelegramBotInfoFlow()
                    .mapNotNull { it.channelId.toChatId() }
                    .onEach { chatId = it }
                    .launchIn(this@channelFlow)
                onContentMessage { message ->
                    if (self.id == message.asFromUser()?.from?.id) {
                        return@onContentMessage
                    }
                    if (message.chat.id.chatId != chatId) {
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
