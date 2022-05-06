package di

import db.DiscordRepository
import db.TelegramRepository
import org.koin.dsl.module

val repositoryModule = module {

    single { DiscordRepository(database = get()) }

    single { TelegramRepository(database = get()) }
}
