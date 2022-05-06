package di

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ResourceLoader
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.StorageClient
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.qualifier.named
import org.koin.dsl.module
import server.*
import update.UpdatesLoader
import update.UpdatesLoaderImpl
import utils.AesUtils

@OptIn(ExperimentalComposeUiApi::class)
val appModule = module {

    single {
        HttpClient {
            Logging {
                logger = Logger.SIMPLE
                level = LogLevel.BODY
            }
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
