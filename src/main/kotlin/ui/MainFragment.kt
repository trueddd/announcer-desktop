package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import di.AppParameters
import di.version
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import navigation.DiscordFragment
import navigation.TelegramFragment
import org.koin.core.component.inject
import update.UpdateAvailability
import update.UpdatesLoader
import utils.AppColor

class MainFragment : Fragment() {

    private val appParameters by inject<AppParameters>()

    private val updatesLoader by inject<UpdatesLoader>()

    private var updateFile by mutableStateOf<UpdateAvailability.HasUpdate?>(null)

    private fun cleanLocalFileForUpdate() {
        updatesLoader.deleteLocalUpdateFiles()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        LaunchedEffect(this) {
            cleanLocalFileForUpdate()
            checkForUpdates()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColor.PrimaryBackground)
        ) {
            updateFile?.let {
                UpdateHeader(it)
            }
            MainContent()
            Footer()
        }
    }

    private fun CoroutineScope.checkForUpdates() {
        updatesLoader.checkForUpdate()
            .onStart { updateFile = null }
            .onEach { status ->
                when (status) {
                    is UpdateAvailability.Checking -> {
                        println("Checking update...")
                    }
                    is UpdateAvailability.Error -> {
                        println("Update check error: ${status.cause.cause}")
                    }
                    is UpdateAvailability.HasUpdate -> {
                        updateFile = status
                    }
                    is UpdateAvailability.NoUpdate -> {
                        updateFile = null
                    }
                }
            }
            .launchIn(this)
    }

    @Composable
    private fun ColumnScope.UpdateHeader(status: UpdateAvailability.HasUpdate) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "New version is available - ${status.version}",
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(8.dp),
            )
            Text(
                text = if (status is UpdateAvailability.HasUpdate.Local) "Local" else "Remote",
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(8.dp),
            )
        }
    }

    @Composable
    private fun ColumnScope.MainContent() {
        Row(
            modifier = Modifier
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                DiscordFragment().Content()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(AppColor.TelegramBackground),
            ) {
                TelegramFragment().Content()
            }
        }
    }

    @Composable
    private fun ColumnScope.Footer() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Only text messages are currently supported.",
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Text(
                text = "v${appParameters.version}",
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}
