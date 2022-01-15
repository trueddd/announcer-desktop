package ui

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent

abstract class Fragment : KoinComponent {

    val lifecycleScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    @Composable
    abstract fun Content()

    open fun onStop() {
        lifecycleScope.cancel()
    }
}
