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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject
import server.ConnectionState
import ui.DiscordViewModel
import ui.Fragment
import ui.common.ConnectionStatus
import ui.common.PasswordVisibilityIndicator
import utils.AppColor

@OptIn(ExperimentalComposeUiApi::class)
class DiscordFragment : Fragment() {

    private val viewModel by inject<DiscordViewModel>()

    private var botInfo by mutableStateOf(viewModel.botInfoFlow.value)

    private var discordConnectionState by mutableStateOf<ConnectionState>(ConnectionState.Disconnected)

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
                onClick = {
                    println(discordConnectionState.toString())
                    if (discordConnectionState is ConnectionState.Disconnected) {
                        viewModel.connectDiscord()
                    } else {
                        viewModel.disconnect()
                    }
                },
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
                value = botInfo.token,
                onValueChange = {
                    botInfo = botInfo.copy(token = it)
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
                val guild = botInfo.guild
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
                    val guilds by viewModel.guildsFlow.collectAsState(lifecycleScope.coroutineContext)
                    guilds.forEach { guild ->
                        DropdownMenuItem(
                            onClick = {
                                viewModel.updateGuild(guild)
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
            val editable = discordConnectionState is ConnectionState.Connected && botInfo.guild != null
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .clickable(editable) { channelsOpen = !channelsOpen }
                    .pointerHoverIcon(if (editable) PointerIconDefaults.Hand else PointerIconDefaults.Default)
            ) {
                val channel = botInfo.channel
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
                    val channels by viewModel.channelsFlow.collectAsState(lifecycleScope.coroutineContext)
                    channels.forEach { channel ->
                        DropdownMenuItem(
                            onClick = {
                                viewModel.updateChannel(channel)
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
            viewModel.discordConnectionState
                .onEach { discordConnectionState = it }
                .launchIn(lifecycleScope)
            viewModel.botInfoFlow
                .onEach { info ->
                    println(info.toString())
                    botInfo = info
                }
                .launchIn(lifecycleScope)
        }
        DiscordPanel()
    }
}
