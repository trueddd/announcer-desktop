package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import db.AppDatabase
import di.repositoryModule
import navigation.MainNavigator
import org.koin.core.context.startKoin
import utils.AppColor

class SplashFragment(
    private val navigator: MainNavigator,
) : Fragment() {

    @Composable
    override fun Content() {
        LaunchedEffect(this) {
            AppDatabase.setup()
            repositoryModule.single(createdAtStart = true) { AppDatabase.instance!! }
            startKoin {
                modules(*di.modules)
            }
            navigator.main()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColor.PrimaryBackground),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                bitmap = useResource("PeepoGlad.png") { loadImageBitmap(it) },
                contentDescription = "Logo",
            )
        }
    }
}
