package utils

import java.io.File

val AppDataFolder: File
    get() = File("${System.getenv("APPDATA")}/announcer")

val AppLogsFolder: File
    get() = File("${System.getenv("APPDATA")}/announcer/logs")
