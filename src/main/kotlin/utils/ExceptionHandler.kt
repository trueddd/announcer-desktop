package utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import java.awt.Window
import java.awt.event.WindowEvent
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
val ExceptionHandler = LocalWindowExceptionHandlerFactory provides object : WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: Window) = WindowExceptionHandler {
        val timestamp = DateTime.now().format(DateFormat("yyyy-MM-dd-HH-mm-ss"))
        File(AppLogsFolder, "crash-$timestamp.txt").apply {
            createNewFile()
            writeText(it.stackTraceToString())
        }
        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
    }
}
