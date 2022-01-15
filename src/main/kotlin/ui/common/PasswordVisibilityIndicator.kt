package ui.common

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import utils.AppColor

@Composable
fun PasswordVisibilityIndicator(
    isVisible: Boolean,
    tint: Color = AppColor.DarkAccent,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = if (isVisible) painterResource("outline_visibility_black_24dp.png") else painterResource("outline_visibility_off_black_24dp.png"),
        contentDescription = "Visibility indicator",
        tint = tint,
        modifier = modifier,
    )
}