import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Versions.Kotlin
    id("org.jetbrains.compose") version Versions.Compose
    id("org.jetbrains.kotlin.plugin.serialization") version Versions.Kotlin
}

group = Config.PackageName
version = Config.Version

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jitpack.io")
}

dependencies {
    // core dependencies
    implementation(compose.desktop.currentOs)
    implementation(compose.uiTooling)
    implementation(Dependency.Ktor.Serialization)
    implementation(Dependency.Ktor.Client.Core)
    implementation(Dependency.Ktor.Client.Java)
    implementation(Dependency.Ktor.Client.Serialization)
    implementation(Dependency.Ktor.Client.Logging)
    implementation(Dependency.Ktor.Server.Core)
    implementation(Dependency.Ktor.Server.Netty)
    implementation(Dependency.Koin.Core)
    // database dependencies
    implementation(Dependency.Exposed.Core)
    implementation(Dependency.Exposed.Dao)
    implementation(Dependency.Exposed.Jdbc)
    implementation(Dependency.Exposed.JavaTime)
    implementation(Dependency.H2)
    // service integrations dependencies
    implementation(Dependency.Kord.Core)
    implementation(Dependency.TelegramBotApi)
    implementation(Dependency.Firebase.Admin)
    // test dependencies
    testImplementation(Dependency.Junit.Jupiter)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "15"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "MainKt"
        args += listOf(
            "version=${Config.Version}",
            "firebaseBucket=${System.getenv("STORAGE_BUCKET")}",
            "encryptionKey=${System.getenv("ENCRYPTION_SECRET_KEY")}",
        )
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = Config.Windows.PackageName
            packageVersion = Config.Version
            modules("java.sql")
            windows {
                upgradeUuid = Config.Windows.UpgradeUuid
                dirChooser = true
                iconFile.set(project.file("src/main/resources/PeepoGlad.ico"))
            }
        }
    }
}
