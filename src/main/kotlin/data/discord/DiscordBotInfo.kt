package data.discord

import db.DiscordBotTable
import org.jetbrains.exposed.sql.ResultRow

data class DiscordBotInfo(
    val token: String = "",
    val guild: Guild? = null,
    val channel: Channel? = null,
) {

    companion object {

        fun ResultRow.asDiscordBotInfo(): DiscordBotInfo {
            val guildId = this[DiscordBotTable.guildId]
            val guildName = this[DiscordBotTable.guildName]
            val guild = if (guildId != null && guildName != null) {
                Guild(guildId, guildName)
            } else {
                null
            }
            val channelId = this[DiscordBotTable.channelId]
            val channelName = this[DiscordBotTable.channelName]
            val channel = if (channelId != null && channelName != null) {
                Channel(channelId, channelName)
            } else {
                null
            }
            return DiscordBotInfo(
                this[DiscordBotTable.token],
                guild,
                channel,
            )
        }
    }
}
