package ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun RowScope.ChooserTitle(
    title: String,
) {
    Text(
        text = title,
        fontSize = 20.sp,
        modifier = Modifier
            .align(Alignment.CenterVertically)
    )
}
