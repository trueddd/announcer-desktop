package navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.discord.DiscordBotInfo
import db.DiscordRepository
import dev.kord.common.entity.DiscordChannel
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import server.Client
import server.ConnectionState
import server.DiscordClient
import ui.Fragment
import ui.common.ConnectionStatus
import ui.common.PasswordVisibilityIndicator
import utils.AppColor

@OptIn(ExperimentalComposeUiApi::class)
class DiscordFragment(
    private val commonMessagesFlow: MutableSharedFlow<Pair<String, String>>,
) : Fragment() {

    private val discordRepository by inject<DiscordRepository>()
    private var discordBotInfo by mutableStateOf(DiscordBotInfo())
    private var discordConnectionState by mutableStateOf<ConnectionState>(ConnectionState.Disconnected)
    private var discordJob: Job? = null

    var guilds by mutableStateOf(emptyList<Guild>())
    var channels by mutableStateOf(emptyList<DiscordChannel>())

    private fun connectDiscord() {
        val client = get<Client>(named(Client.Type.Discord)) { parametersOf(discordBotInfo) }
        discordJob = lifecycleScope.launch {
            client.start()
                .onEach {
                    commonMessagesFlow.tryEmit(Client.Type.Discord to it)
                }
                .launchIn(this)
            client.state
                .onEach { discordConnectionState = it }
                .filterIsInstance<ConnectionState.Connected>()
                .onEach { guilds = (client as DiscordClient).getGuilds() }
                .onCompletion { discordConnectionState = ConnectionState.Disconnected }
                .launchIn(this)
            commonMessagesFlow
                .transform { (client, message) ->
                    if (client != Client.Type.Discord) {
                        emit(message)
                    }
                }
                .onEach { client.send(it) }
                .launchIn(this)
            client.state
                .filterIsInstance<ConnectionState.Connected>()
                .combine(discordRepository.getDiscordBotInfoFlow()) { _, botInfo -> botInfo }
                .mapNotNull { it.guild }
                .distinctUntilChanged()
                .onEach { channels = (client as DiscordClient).getChannels(Snowflake(it.id)) }
                .launchIn(this)
        }
    }

    private fun onDiscordButtonClicked() {
        when {
            discordJob?.isActive == true -> {
                discordJob?.cancel()
                discordJob = null
            }
            discordBotInfo.token.isNotEmpty() -> {
                connectDiscord()
            }
            else -> {
                println("NOT VALID")
            }
        }
    }

    @Composable
    private fun DiscordPanel() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(AppColor.Discord)
        ) {
            Image(
                bitmap = useResource("discord_logo.png") { loadImageBitmap(it) },
                contentDescription = "Discord Logo",
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            ConnectionStatus(
                discordConnectionState,
                modifier = Modifier
                    .align(Alignment.CenterVertically),
            )
            Button(
                onClick = ::onDiscordButtonClicked,
            ) {
                Text(
                    text = if (discordConnectionState is ConnectionState.Disconnected) "Start" else "Stop",
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            var showToken by remember { mutableStateOf(false) }
            TextField(
                value = discordBotInfo.token,
                onValueChange = {
                    discordBotInfo = discordBotInfo.copy(token = it)
                    lifecycleScope.launch { discordRepository.updateToken(it) }
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
        Spacer(modifier = Modifier.height(8.dp))
        GuildChooser()
        ChannelChooser()
    }

    @Composable
    private fun ChooserRow(
        content: @Composable RowScope.() -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) { content.invoke(this) }
    }

    @Composable
    private fun RowScope.ChooserTitle(
        title: String,
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.CenterVertically)
        )
    }

    @Composable
    private fun BoxScope.ChooserPlaceholder() {
        Text(
            text = "Click here to choose...",
            color = AppColor.Primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
        )
    }

    @Composable
    private fun BoxScope.ChooserValue(
        value: String,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .background(AppColor.DarkBackground, RoundedCornerShape(4.dp))
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = AppColor.White,
                modifier = Modifier
                    .padding(4.dp)
            )
        }
    }

    @Composable
    private fun GuildChooser() {
        ChooserRow {
            var guildsOpen by remember { mutableStateOf(false) }
            ChooserTitle(title = "Guild")
            val editable = discordConnectionState is ConnectionState.Connected
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .clickable(editable) { guildsOpen = !guildsOpen }
                    .pointerHoverIcon(if (editable) PointerIconDefaults.Hand else PointerIconDefaults.Default)
            ) {
                val guild = discordBotInfo.guild
                if (guild == null) {
                    ChooserPlaceholder()
                } else {
                    ChooserValue(value = guild.name)
                }
                DropdownMenu(
                    expanded = guildsOpen,
                    onDismissRequest = { guildsOpen = false },
                    modifier = Modifier
                        .background(AppColor.DarkBackground)
                ) {
                    guilds.forEach { guild ->
                        DropdownMenuItem(
                            onClick = {
                                lifecycleScope.launch { discordRepository.updateGuild(guild.id.value.toString(), guild.name) }
                                guildsOpen = false
                            }
                        ) {
                            Text(text = guild.name)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ChannelChooser() {
        ChooserRow {
            var channelsOpen by remember { mutableStateOf(false) }
            ChooserTitle(title = "Channel")
            val editable = discordConnectionState is ConnectionState.Connected && discordBotInfo.guild != null
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .clickable(editable) { channelsOpen = !channelsOpen }
                    .pointerHoverIcon(if (editable) PointerIconDefaults.Hand else PointerIconDefaults.Default)
            ) {
                val channel = discordBotInfo.channel
                if (channel == null) {
                    ChooserPlaceholder()
                } else {
                    ChooserValue(value = "#${channel.name}")
                }
                DropdownMenu(
                    expanded = channelsOpen,
                    onDismissRequest = { channelsOpen = false },
                    modifier = Modifier
                        .background(AppColor.DarkBackground)
                ) {
                    channels.forEach { channel ->
                        DropdownMenuItem(
                            onClick = {
                                lifecycleScope.launch { discordRepository.updateChannel(channel.id.value.toString(), channel.name.value!!) }
                                channelsOpen = false
                            }
                        ) {
                            Text(text = "#${channel.name.value!!}")
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(this) {
            discordRepository.getDiscordBotInfoFlow()
                .withIndex()
                .onEach { (index, info) ->
                    println(info.toString())
                    discordBotInfo = info
                    if (index == 0 && info.token.isNotEmpty()) {
                        connectDiscord()
                    }
                }
                .launchIn(lifecycleScope)
        }
        DiscordPanel()
    }
}
