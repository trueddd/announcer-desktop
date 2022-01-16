import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.create

fun DependencyHandler.ktor(name: String) = create("io.ktor", name, Versions.Ktor)
fun DependencyHandler.okHttp(name: String) = create("com.squareup.okhttp3", name, "4.9.1")
fun DependencyHandler.moshi(name: String) = create("com.squareup.moshi", name, "1.12.0")
fun DependencyHandler.exposed(name: String) = create("org.jetbrains.exposed", name, Versions.Exposed)
