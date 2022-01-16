package ui

import data.discord.DiscordBotInfo
import db.DiscordRepository
import dev.kord.common.entity.DiscordChannel
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import di.MessagesFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import server.Client
import server.ConnectionState
import server.DiscordClient

class DiscordViewModel(
    private val messagesFlow: MessagesFlow,
    private val discordRepository: DiscordRepository,
    private val discordClient: DiscordClient,
) : BaseViewModel() {

    private var discordJob: Job? = null

    val discordConnectionState = discordClient.state.stateIn(this, SharingStarted.Eagerly, ConnectionState.Disconnected)

    val guildsFlow = discordConnectionState
        .filterIsInstance<ConnectionState.Connected>()
        .map { discordClient.getGuilds() }
        .onEach { guilds ->
            if (guilds.isNotEmpty() && guilds.none { it.id.value.toString() == selectedGuild.value?.id }) {
                discordRepository.dropGuild()
            }
        }
        .stateIn(this, SharingStarted.Eagerly, emptyList())

    val botInfoFlow = discordRepository.getDiscordBotInfoFlow()
        .stateIn(this, SharingStarted.Eagerly, DiscordBotInfo())

    val selectedGuild = botInfoFlow
        .map { it.guild }
        .stateIn(this, SharingStarted.Eagerly, null)

    val channelsFlow = discordConnectionState
        .transformLatest {
            if (it is ConnectionState.Connected) {
                selectedGuild
                    .map { guild ->
                        if (guild == null) {
                            return@map emptyList()
                        }
                        discordClient.getChannels(Snowflake(guild.id))
                    }
                    .collect(this)
            } else {
                emit(emptyList())
            }
        }
        .stateIn(this, SharingStarted.Eagerly, emptyList())

    fun disconnect() {
        if (discordJob?.isActive == true) {
            discordJob?.cancel()
            discordJob = null
        }
    }

    fun connectDiscord() {
        if (botInfoFlow.value.token.isEmpty()) {
            println("NOT VALID")
            return
        }
        discordJob = launch {
            discordClient.start()
                .onEach { messagesFlow.tryEmit(Client.Type.Discord to it) }
                .launchIn(this)
            messagesFlow
                .filter { (client, _) -> client != Client.Type.Discord }
                .onEach { (_, message) -> discordClient.send(message) }
                .launchIn(this)
        }
    }

    fun updateToken(token: String) = launch { discordRepository.updateToken(token) }

    fun updateGuild(guild: Guild) = launch { discordRepository.updateGuild(guild.id.value.toString(), guild.name) }

    fun updateChannel(channel: DiscordChannel) = launch { discordRepository.updateChannel(channel.id.value.toString(), channel.name.value!!) }
}
