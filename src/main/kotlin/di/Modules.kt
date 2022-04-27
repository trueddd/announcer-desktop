package di

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.StorageClient
import db.DiscordRepository
import db.TelegramRepository
import io.ktor.client.*
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
import update.UpdatesLoader
import update.UpdatesLoaderImpl
import utils.AesUtils

val repositoryModule = module {

    single { DiscordRepository(database = get()) }

    single { TelegramRepository(database = get()) }
}

@OptIn(ExperimentalComposeUiApi::class)
val appModule = module {

    single {
        HttpClient {
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

    single<TelegramClient>(named(Client.Type.Telegram)) {
        TelegramClientImpl(telegramRepository = get())
    }

    single {
        val bucketName = get<AppParameters>().firebaseBucket
        val keyStream = ResourceLoader.Default.load("firebase-service.key")
        val serviceKey = AesUtils.decrypt(keyStream.readBytes().decodeToString(), get<AppParameters>().encryptionKey)
        val options = FirebaseOptions
            .builder()
            .setCredentials(GoogleCredentials.fromStream(serviceKey.byteInputStream()))
            .setStorageBucket(bucketName)
            .build()
        FirebaseApp.initializeApp(options)
    }

    single { StorageClient.getInstance(get()).bucket() }

    single<UpdatesLoader> { UpdatesLoaderImpl(appParameters = get(), firebaseBucket = get()) }
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
typealias AppStopper = () -> Unit

typealias AppParameters = Map<String, String>
val AppParameters.version: String
    get() = this["version"]!!
private val AppParameters.encryptionKey: String
    get() = this["encryptionKey"]!!
private val AppParameters.firebaseBucket: String
    get() = this["firebaseBucket"]!!
