package db

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.exposed.sql.Table

abstract class BaseTable(name: String) : Table(name) {

    private val _invalidationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val invalidationEvents: SharedFlow<String>
        get() = _invalidationEvents

    fun invalidate() = _invalidationEvents.tryEmit(tableName)
}
