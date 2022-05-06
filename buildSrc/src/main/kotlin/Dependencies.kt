import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.create

fun DependencyHandler.implementation(dependency: Dependency) {
    add("implementation", create(dependency.group, dependency.module, dependency.version))
}

fun DependencyHandler.testImplementation(dependency: Dependency) {
    add("testImplementation", create(dependency.group, dependency.module, dependency.version))
}

sealed class Dependency(
    open val group: String,
    open val module: String,
    open val version: String,
) {

    sealed class Ktor(val ktorModule: String) : Dependency("io.ktor", "ktor-$ktorModule", "1.6.5") {
        object Serialization : Ktor("serialization")
        sealed class Server(val serverModule: String) : Ktor("server-$serverModule") {
            object Core : Server("core")
            object Netty : Server("netty")
        }
        sealed class Client(val clientModule: String) : Ktor("client-$clientModule") {
            object Core : Client("core")
            object Java : Client("java")
            object Logging : Client("logging")
            object Serialization : Client("serialization")
        }
    }

    sealed class Koin(val koinModule: String) : Dependency("io.insert-koin", "koin-$koinModule", "3.1.5") {
        object Core : Koin("core")
    }

    sealed class Exposed(val exposedModule: String) : Dependency("org.jetbrains.exposed", "exposed-$exposedModule", "0.37.3") {
        object Core : Exposed("core")
        object Dao : Exposed("dao")
        object Jdbc : Exposed("jdbc")
        object JavaTime : Exposed("java-time")
    }

    object H2 : Dependency("com.h2database", "h2", "1.4.199")

    sealed class Kord(val kordModule: String) : Dependency("dev.kord", "kord-$kordModule", "0.8.0-M8") {
        object Core : Kord("core")
    }

    object TelegramBotApi : Dependency("dev.inmo", "tgbotapi", "0.38.7")

    sealed class Firebase(val firebaseModule: String) : Dependency("com.google.firebase", "firebase-$firebaseModule", "8.1.0") {
        object Admin : Firebase("admin")
    }

    sealed class Junit(val junitModule: String) : Dependency("org.junit.jupiter", "junit-$junitModule", "5.8.2") {
        object Jupiter : Junit("jupiter")
    }
}
