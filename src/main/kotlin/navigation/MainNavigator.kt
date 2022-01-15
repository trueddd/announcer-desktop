package navigation

import androidx.compose.runtime.*
import ui.Fragment
import ui.MainFragment
import ui.SplashFragment

class MainNavigator : BaseNavigator() {

    override var fragment by mutableStateOf<Fragment>(SplashFragment(this))

    fun main() = navigate(MainFragment())
}
