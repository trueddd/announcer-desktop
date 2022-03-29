package update

import java.io.File

sealed class UpdateStatus {

    data class Error(
        val cause: Throwable,
    ) : UpdateStatus()

    data class Downloading(
        val loaded: Long,
        val total: Long,
    ) : UpdateStatus()

    data class Downloaded(
        val file: File,
    ) : UpdateStatus()
}
