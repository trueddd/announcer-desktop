package db

import data.discord.DiscordBotInfo
import data.discord.DiscordBotInfo.Companion.asDiscordBotInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class DiscordRepository(database: Database) : BaseRepository(database) {

    fun getDiscordBotInfoFlow(): Flow<DiscordBotInfo> {
        return DiscordBotTable.queryAsFlow(database) {
            DiscordBotTable.selectAll().limit(1).firstOrNull()?.asDiscordBotInfo()!!
        }
            .onStart { initInfo() }
    }

    private suspend fun initInfo() {
        dbCall {
            val hasRecord = DiscordBotTable.selectAll().count().let { it > 0 }
            if (!hasRecord) {
                DiscordBotTable.insert {
                    it[token] = ""
                }
            }
        }
    }

    suspend fun updateToken(token: String) {
        dbCall(DiscordBotTable) {
            DiscordBotTable.update {
                it[DiscordBotTable.token] = token
            }
        }
    }

    suspend fun updateGuild(id: String, name: String) {
        dbCall(DiscordBotTable) {
            DiscordBotTable.update {
                it[guildId] = id
                it[guildName] = name
                it[channelId] = null
                it[channelName] = null
            }
        }
    }

    suspend fun updateChannel(id: String, name: String) {
        dbCall(DiscordBotTable) {
            DiscordBotTable.update {
                it[channelId] = id
                it[channelName] = name
            }
        }
    }
}
