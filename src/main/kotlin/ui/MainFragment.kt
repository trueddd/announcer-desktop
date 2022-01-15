package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.*
import navigation.DiscordFragment
import navigation.TelegramFragment
import utils.AppColor

class MainFragment : Fragment() {

    private val commonMessagesFlow = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 1)

    @Composable
    override fun Content() {
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
                    DiscordFragment(commonMessagesFlow).Content()
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(AppColor.TelegramBackground),
                ) {
                    TelegramFragment(commonMessagesFlow).Content()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Only text messages are currently supported.",
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.Center),
                )
            }
        }
    }
}
