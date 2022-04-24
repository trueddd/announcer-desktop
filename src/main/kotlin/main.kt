import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.*
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import di.AppStopper
import di.appModule
import navigation.MainNavigator
import java.awt.Window
import java.awt.event.WindowEvent
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
fun main(vararg args: String) = application {
    val appParams = getApplicationParameter(args)
    CompositionLocalProvider(
        LocalWindowExceptionHandlerFactory provides object : WindowExceptionHandlerFactory {
            override fun exceptionHandler(window: Window) = WindowExceptionHandler {
                val crashLogFile = File("${System.getenv("APPDATA")}/announcer/crash-${DateTime.now().format(DateFormat("yyyy-MM-dd-HH-mm-ss"))}.txt")
                crashLogFile.createNewFile()
                crashLogFile.writeText(it.stackTraceToString())
                window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
            }
        }
    ) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Announcer",
        ) {
            println("App launched with parameters: $appParams")
            appModule.single<AppStopper> { ::exitApplication }
            val navigator = MainNavigator(appParams)
            MaterialTheme {
                navigator.fragment.Content()
            }
        }
    }
}

private fun getApplicationParameter(args: Array<out String>): Map<String, String> {
    return args.associate {
        val (name, value) = it.split("=", limit = 2)
        name to value
    }
}
