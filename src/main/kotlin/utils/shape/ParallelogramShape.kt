package utils.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.tan

class ParallelogramShape(
    private val angle: Float,
) : Shape {

    init {
        require(abs(angle) <= 90f) { "Angle must be in range from -90 to 90 degrees" }
    }

    private fun calculateShift(size: Size): Float {
        return size.height / tan(angle)
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(calculateShift(size), 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - calculateShift(size), size.height)
            lineTo(0f, size.height)
        }
        return Outline.Generic(path)
    }
}
