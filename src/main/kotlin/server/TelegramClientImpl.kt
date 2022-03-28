package server

import db.TelegramRepository
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.asChannelChat
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.shortcuts.events
import dev.inmo.tgbotapi.extensions.utils.shortcuts.filterChannelEvents
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.chat.abstracts.ChannelChat
import dev.inmo.tgbotapi.types.message.ChatEvents.PinnedMessage
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramClientImpl(
    private val telegramRepository: TelegramRepository,
) : TelegramClient, CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        SupervisorJob() + Dispatchers.IO
    }

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState>
        get() = _state

    private var botBehaviour: BehaviourContext? = null

    private var chatId: Long? = null

    private fun String.normalizeChatId(): Long? {
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

    @OptIn(RiskFeature::class, PreviewFeature::class)
    override fun waitForChatWithPinnedMessage(): Flow<ChannelChat> {
        val client = botBehaviour ?: return flow { throw IllegalStateException("No bot object found.") }
        return client.events()
            .filterChannelEvents<PinnedMessage>()
            .take(1)
            .mapNotNull { it.chat.asChannelChat() }
            .onEach { println("${it.id} ${it.title}") }
    }

    @OptIn(PreviewFeature::class)
    override fun start(): Flow<String> {
        return channelFlow {
            _state.value = ConnectionState.Connecting
            val token = telegramRepository.getTelegramBotInfoFlow().first().token
            telegramBotWithBehaviourAndLongPolling(token, this@TelegramClientImpl) {
                val self = getMe()
                botBehaviour = this
                _state.value = ConnectionState.Connected
                telegramRepository.getTelegramBotInfoFlow()
                    .mapNotNull { it.channelId.normalizeChatId() }
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
