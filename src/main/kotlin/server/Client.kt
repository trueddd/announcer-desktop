package server

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Client {

    object Type {
        const val Telegram = "telegram"
        const val Discord = "discord"
    }

    val state: StateFlow<ConnectionState>

    fun start(): Flow<String>

    fun send(message: String)
}
