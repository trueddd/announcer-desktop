package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import di.AppParameters
import di.AppStopper
import di.version
import navigation.DiscordFragment
import navigation.TelegramFragment
import org.koin.core.component.get
import org.koin.core.component.inject
import update.UpdateAvailability
import update.UpdatesLoader
import utils.AppColor
import java.awt.Desktop

class MainFragment : Fragment() {

    private val appParameters by inject<AppParameters>()

    private val updatesLoader by inject<UpdatesLoader>()

    private fun cleanLocalFileForUpdate() {
        updatesLoader.deleteLocalUpdateFiles()
    }

    @Composable
    override fun Content() {
        val updateStatus by updatesLoader.updateAvailabilityFlow.collectAsState(lifecycleScope)
        LaunchedEffect(this) {
            cleanLocalFileForUpdate()
            checkForUpdates()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColor.PrimaryBackground)
        ) {
            AnimatedVisibility(updateStatus is UpdateAvailability.HasUpdate) {
                UpdateHeader(updateStatus as UpdateAvailability.HasUpdate)
            }
            MainContent()
            Footer()
        }
    }

    private fun checkForUpdates() {
        updatesLoader.checkForUpdate()
    }

    @Composable
    private fun UpdateHeader(status: UpdateAvailability.HasUpdate) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "New version is available - ${status.updateData.version}",
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(8.dp),
            )
            when (status) {
                is UpdateAvailability.HasUpdate.ReadyToUpdate -> ReadyToUpdate(status)
                is UpdateAvailability.HasUpdate.ReadyToDownload -> ReadyToDownload(status)
                is UpdateAvailability.HasUpdate.Downloading -> UpdateStatus(status)
                is UpdateAvailability.HasUpdate.DownloadError -> UpdateError(status)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun UpdateButton(
        text: String,
        action: () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        Box(
            modifier = Modifier
                .clickable(
                    onClick = action,
                    indication = rememberRipple(),
                    interactionSource = interactionSource,
                )
                .pointerHoverIcon(PointerIconDefaults.Hand)
                .background(
                    if (isHovered) AppColor.DarkAccent.copy(alpha = 0.5f) else AppColor.DarkAccent,
                    RoundedCornerShape(4.dp)
                )
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                color = AppColor.White,
                modifier = Modifier
                    .padding(4.dp)
            )
        }
    }

    @Composable
    private fun ReadyToDownload(status: UpdateAvailability.HasUpdate.ReadyToDownload) {
        val mbFileSize = status.updateData.file.size / 1024f / 1024
        UpdateButton("Download (${String.format("%.1f MB", mbFileSize)})") {
            updatesLoader.loadUpdate(status.updateData)
        }
    }

    @Composable
    private fun ReadyToUpdate(status: UpdateAvailability.HasUpdate.ReadyToUpdate) {
        UpdateButton("Update") {
            Desktop.getDesktop().open(status.updateData.file)
            get<AppStopper>().invoke()
        }
    }

    @Composable
    private fun UpdateStatus(status: UpdateAvailability.HasUpdate.Downloading) {
        Text(
            text = "Downloading - ${String.format("%.1f", status.downloaded * 100f / status.updateData.file.size)}%",
            fontSize = 14.sp,
            color = AppColor.White,
            modifier = Modifier
                .padding(4.dp)
        )
    }

    @Composable
    private fun UpdateError(status: UpdateAvailability.HasUpdate.DownloadError) {
        Text(
            text = "Download error - ${status.cause.message}",
            fontSize = 14.sp,
            color = AppColor.White,
            modifier = Modifier
                .padding(4.dp)
        )
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
    private fun Footer() {
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
