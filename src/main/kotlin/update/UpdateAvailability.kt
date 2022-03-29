package update

import com.google.cloud.storage.Blob
import java.io.File

sealed class UpdateAvailability {

    data class Error(
        val cause: Throwable,
    ) : UpdateAvailability()

    object Checking : UpdateAvailability()

    object NoUpdate : UpdateAvailability()

    sealed class HasUpdate(open val version: String) : UpdateAvailability() {

        data class Local(val file: File, override val version: String) : HasUpdate(version)

        data class Remote(val file: Blob, override val version: String) : HasUpdate(version)
    }
}
