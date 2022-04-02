package data.telegram

import db.TelegramBotTable
import org.jetbrains.exposed.sql.ResultRow

data class TelegramBotInfo(
    val token: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
) {

    val canConnect = token.isNotEmpty()

    companion object {

        fun ResultRow.asTelegramBotInfo(): TelegramBotInfo {
            return TelegramBotInfo(
                this[TelegramBotTable.token],
                this[TelegramBotTable.channelId],
                this[TelegramBotTable.channelTitle],
            )
        }
    }
}
