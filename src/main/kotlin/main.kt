import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import navigation.MainNavigator

fun main() = application {

    Window(
        onCloseRequest = ::exitApplication,
        title = "Announcer",
    ) {

        val navigator = MainNavigator()

        MaterialTheme {
            navigator.fragment.Content()
        }
    }
}
