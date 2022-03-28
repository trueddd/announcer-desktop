package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import di.AppParameters
import di.version
import navigation.DiscordFragment
import navigation.TelegramFragment
import org.koin.core.component.inject
import utils.AppColor

class MainFragment : Fragment() {

    private val appParameters by inject<AppParameters>()

    private val firebaseBucket by inject<Bucket>()

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        LaunchedEffect(this) {
            val files = firebaseBucket.list(Storage.BlobListOption.currentDirectory()).values
            println("Found files on bucket: ${files.joinToString { it.name }}")
            // TODO: implement files loader via storage reader
            //  firebaseBucket.storage.reader(firebaseBucket.name, file.name)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColor.PrimaryBackground)
        ) {
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
}
