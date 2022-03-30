import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Versions.Kotlin
    id("org.jetbrains.compose") version "1.1.0"
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
    implementation(compose.desktop.currentOs)
    implementation(compose.uiTooling)
    implementation(ktor("ktor-server-core"))
    implementation(ktor("ktor-serialization"))
    implementation(ktor("ktor-server-netty"))
    implementation(ktor("ktor-client-core"))
    implementation(ktor("ktor-client-java"))
    implementation(ktor("ktor-client-serialization"))
    implementation(ktor("ktor-client-logging"))
    implementation("io.insert-koin:koin-core:3.1.5")

    implementation(exposed("exposed-core"))
    implementation(exposed("exposed-dao"))
    implementation(exposed("exposed-jdbc"))
    implementation(exposed("exposed-java-time"))
    implementation("com.h2database:h2:1.4.199")

    implementation("dev.kord:kord-core:0.8.0-M8")
    implementation("dev.inmo:tgbotapi:0.38.7")

    implementation("com.google.firebase:firebase-admin:8.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "15"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register("uploadPackageToFirebase") {
    dependsOn("packageMsi")
    val targetFile = File("${buildDir.absolutePath}/compose/binaries/main/msi/${Config.Windows.PackageName}-${Config.Version}.msi")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        args += listOf(
            "version=${Config.Version}",
            "firebaseKeyFile=${FirebaseConfig.KeyFileName}",
            "firebaseBucket=${FirebaseConfig.StorageBucket}",
        )
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = Config.Windows.PackageName
            packageVersion = Config.Version
            modules("java.sql")
            windows {
                upgradeUuid = Config.Windows.UpgradeUuid
            }
        }
    }
}
