package di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import server.Client
import ui.DiscordViewModel
import ui.TelegramViewModel

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
