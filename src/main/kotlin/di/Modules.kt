package di

import data.discord.DiscordBotInfo
import data.telegram.TelegramBotInfo
import db.DiscordRepository
import db.TelegramRepository
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.websocket.*
import org.koin.core.qualifier.named
import org.koin.dsl.module
import server.Client
import server.DiscordClientImpl
import server.TelegramClientImpl

val repositoryModule = module {

    single { DiscordRepository(database = get()) }

    single { TelegramRepository(database = get()) }
}

val appModule = module {

    factory {
        HttpClient(Java) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.BODY
            }
            install(WebSockets)
            Json {
                serializer = KotlinxSerializer(
                    kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                )
            }
        }
    }

    factory<Client>(named(Client.Type.Discord)) { (discordInfo: DiscordBotInfo) -> DiscordClientImpl(discordInfo, get()) }

    factory<Client>(named(Client.Type.Telegram)) { (telegramInfo: TelegramBotInfo) -> TelegramClientImpl(telegramInfo) }
}

val modules = arrayOf(repositoryModule, appModule)
