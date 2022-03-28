package server

import dev.inmo.tgbotapi.types.chat.abstracts.ChannelChat
import kotlinx.coroutines.flow.Flow

interface TelegramClient : Client {

    fun waitForChatWithPinnedMessage(): Flow<ChannelChat>
}
