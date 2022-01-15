package db

import data.telegram.TelegramBotInfo
import data.telegram.TelegramBotInfo.Companion.asTelegramBotInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class TelegramRepository(database: Database) : BaseRepository(database) {

    fun getTelegramBotInfoFlow(): Flow<TelegramBotInfo> {
        return TelegramBotTable.queryAsFlow(database) {
            TelegramBotTable.selectAll().limit(1).firstOrNull()?.asTelegramBotInfo()!!
        }
            .onStart { initInfo() }
    }

    private suspend fun initInfo() {
        dbCall {
            val hasRecord = TelegramBotTable.selectAll().count().let { it > 0 }
            if (!hasRecord) {
                TelegramBotTable.insert {
                    it[token] = ""
                }
            }
        }
    }

    suspend fun updateToken(token: String) {
        dbCall(TelegramBotTable) {
            TelegramBotTable.update {
                it[TelegramBotTable.token] = token
            }
        }
    }

    suspend fun updateChannel(channelId: String) {
        dbCall(TelegramBotTable) {
            TelegramBotTable.update {
                it[TelegramBotTable.channelId] = channelId
            }
        }
    }
}
