package server

import db.DiscordRepository
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.DiscordChannel
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class DiscordClientImpl(
    private val discordRepository: DiscordRepository,
) : DiscordClient, CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        SupervisorJob() + Dispatchers.IO
    }

    private var kord: Kord? = null

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState>
        get() = _state

    private var guildId: String? = null
    private var channelId: String? = null

    override fun send(message: String) {
        val client = kord ?: return
        launch {
            channelId?.let { id ->
                client.rest.channel.createMessage(Snowflake(id)) {
                    content = message
                }
            }
        }
    }

    override fun start(): Flow<String> {
        return channelFlow<String> {
            _state.value = ConnectionState.Connecting
            val client = try {
                val token = discordRepository.getDiscordBotInfoFlow()
                    .map { it.token }
                    .first()
                Kord(token).also { kord = it }
            } catch (e: Exception) {
                _state.value = ConnectionState.Disconnected
                throw e
            }
            discordRepository.getDiscordBotInfoFlow()
                .onEach {
                    guildId = it.guild?.id
                    channelId = it.channel?.id
                }
                .launchIn(this)
            client.on<ReadyEvent> {
                _state.value = ConnectionState.Connected
            }
            client.on<MessageCreateEvent> {
                if (this.guildId?.value?.toString() != this@DiscordClientImpl.guildId || this@DiscordClientImpl.guildId == null) {
                    return@on
                }
                if (message.channelId.value.toString() != channelId) {
                    return@on
                }
                if (client.selfId == message.author?.id) {
                    return@on
                }
                send(message.content)
            }
            client.login()
            awaitClose {
                kord = null
                launch {
                    client.shutdown()
                }
            }
        }.onCompletion { _state.value = ConnectionState.Disconnected }
    }

    override suspend fun getGuilds(): List<Guild> {
        val client = kord ?: return emptyList()
        return client.guilds.toList()
    }

    override suspend fun getChannels(guildId: Snowflake): List<DiscordChannel> {
        val client = kord ?: return emptyList()
        return client.rest.guild.getGuildChannels(guildId)
            .also { println("loaded: $it") }
            .filter {
                it.type is ChannelType.GuildText || it.type is ChannelType.GuildNews
            }
    }
}
