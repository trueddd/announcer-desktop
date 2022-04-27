package utils

import java.io.File

val AppDataFolder: File
    get() = File("${System.getenv("APPDATA")}/announcer").also { it.mkdirs() }

val AppLogsFolder: File
    get() = File("${System.getenv("APPDATA")}/announcer/logs").also { it.mkdirs() }
