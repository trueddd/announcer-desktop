package db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

abstract class BaseRepository(
    protected val database: Database,
) {

    protected suspend fun <T> dbCall(vararg editedTables: BaseTable, block: suspend Transaction.() -> T?): T? {
        return suspendedTransactionAsync(Dispatchers.IO, database) {
            try {
                val result = block()
                editedTables.forEach { table -> table.invalidate() }
                result
            } catch (e: Exception) {
                e.printStackTrace()
                return@suspendedTransactionAsync null
            }
        }.await()
    }
}

fun <T : BaseTable, R> T.queryAsFlow(database: Database, query: () -> R): Flow<R> {
    return invalidationEvents
        .onStart { emit("") }
        .conflate()
        .mapNotNull {
            suspendedTransactionAsync(Dispatchers.IO, database) {
                try {
                    query()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@suspendedTransactionAsync null
                }
            }.await()
        }
}
