package server

import dev.kord.common.entity.DiscordChannel
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild

interface DiscordClient : Client {

    suspend fun getGuilds(): List<Guild>

    suspend fun getChannels(guildId: Snowflake): List<DiscordChannel>
}
