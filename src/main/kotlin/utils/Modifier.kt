package utils

import androidx.compose.ui.Modifier

fun Modifier.modifyIf(condition: Boolean, block: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        this.then(block(this))
    } else {
        this
    }
}