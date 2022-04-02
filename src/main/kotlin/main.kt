import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import di.AppStopper
import di.appModule
import navigation.MainNavigator

fun main(vararg args: String) = application {
    val appParams = getApplicationParameter(args)
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

private fun getApplicationParameter(args: Array<out String>): Map<String, String> {
    return args.associate {
        val (name, value) = it.split("=", limit = 2)
        name to value
    }
}
