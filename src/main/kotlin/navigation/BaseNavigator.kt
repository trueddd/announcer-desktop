package navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ui.Fragment
import kotlin.coroutines.CoroutineContext

abstract class BaseNavigator : CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        Dispatchers.Main + SupervisorJob()
    }

    abstract var fragment: Fragment

    fun navigate(fragment: Fragment) {
        this.fragment.onStop()
        this.fragment = fragment
    }
}
