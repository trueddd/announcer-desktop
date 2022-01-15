package db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object AppDatabase {

    var instance: Database? = null
        private set

    private val tables = arrayOf<Table>(
        DiscordBotTable,
        TelegramBotTable,
    )

    suspend fun setup() {
        instance = withContext(Dispatchers.IO) {
            Database.connect("jdbc:h2:${System.getenv("APPDATA")}/announcer/announcer_db", "org.h2.Driver").also {
                transaction {
                    SchemaUtils.createMissingTablesAndColumns(*tables)
                }
            }
        }
    }
}
