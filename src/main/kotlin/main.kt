import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.application
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import di.AppStopper
import di.appModule
import navigation.MainNavigator
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
fun main(vararg args: String) = application {
    val appParams = getApplicationParameter(args)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Announcer",
    ) {
        LaunchedEffect(Unit) {
            window.exceptionHandler = WindowExceptionHandler {
                val crashLogFile = File("${System.getenv("APPDATA")}/announcer/logs", "crash-${DateTime.now().format(DateFormat.FORMAT1)}.txt")
                crashLogFile.createNewFile()
                crashLogFile.writeText(it.stackTraceToString())
            }
        }
        println("App launched with parameters: $appParams")
        appModule.single<AppStopper> { ::exitApplication }
        val navigator = MainNavigator(appParams)
        MaterialTheme {
            navigator.fragment.Content()
        }
    }
}

private fun getApplicationParameter(args: Array<out String>): Map<String, String> {
    return args.associate {
        val (name, value) = it.split("=", limit = 2)
        name to value
    }
}
