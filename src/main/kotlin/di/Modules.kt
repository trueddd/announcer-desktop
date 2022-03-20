package di

import db.DiscordRepository
import db.TelegramRepository
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.qualifier.named
import org.koin.dsl.module
import server.*
import ui.DiscordViewModel
import ui.TelegramViewModel

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

    single<MessagesFlow> { MutableSharedFlow(extraBufferCapacity = 1) }

    single<DiscordClient>(named(Client.Type.Discord)) {
        DiscordClientImpl(discordRepository = get())
    }

    single<TelegramClient>(named(Client.Type.Telegram)) { //(telegramInfo: TelegramBotInfo) ->
        TelegramClientImpl(telegramRepository = get())
    }
}

val viewModelModule = module {

    factory {
        DiscordViewModel(
            messagesFlow = get(),
            discordRepository = get(),
            discordClient = get(named(Client.Type.Discord)),
        )
    }

    factory {
        TelegramViewModel(
            messagesFlow = get(),
            telegramRepository = get(),
            telegramClient = get(named(Client.Type.Telegram)),
        )
    }
}

val modules = arrayOf(repositoryModule, appModule, viewModelModule)

typealias MessageSource = String
typealias Content = String
typealias MessagesFlow = MutableSharedFlow<Pair<MessageSource, Content>>
