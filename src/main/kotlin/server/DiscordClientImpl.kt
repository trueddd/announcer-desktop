package server

import data.discord.DiscordBotInfo
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
    private val discordBotInfo: DiscordBotInfo,
    private val discordRepository: DiscordRepository,
) : DiscordClient, CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        SupervisorJob() + Dispatchers.IO
    }

    private var kord: Kord? = null

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState>
        get() = _state

    override fun send(message: String) {
        val client = kord ?: return
        launch {
            if (discordBotInfo.channel?.id != null) {
                client.rest.channel.createMessage(Snowflake(discordBotInfo.channel.id)) {
                    content = message
                }
            }
        }
    }

    override fun start(): Flow<String> {
        return channelFlow {
            _state.value = ConnectionState.Connecting
            val client = try {
                Kord(discordBotInfo.token).also { kord = it }
            } catch (e: Exception) {
                _state.value = ConnectionState.Disconnected
                throw e
            }
            var guildId = discordBotInfo.guild?.id
            var channelId = discordBotInfo.channel?.id
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
                if (this.guildId?.value?.toString() != guildId) {
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
                _state.value = ConnectionState.Disconnected
                kord = null
                launch {
                    client.shutdown()
                }
            }
        }
    }

    override suspend fun getGuilds(): List<Guild> {
        val client = kord ?: return emptyList()
        return client.guilds.toList()
    }

    override suspend fun getChannels(guildId: Snowflake): List<DiscordChannel> {
        val client = kord ?: return emptyList()
        return client.rest.guild.getGuildChannels(guildId)
            .also { println("loaded: $it") }
            .filter { it.type is ChannelType.GuildText }
    }
}
