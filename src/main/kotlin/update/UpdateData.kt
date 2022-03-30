package update

import com.google.cloud.storage.Blob
import java.io.File

sealed class UpdateData(
    open val version: String,
) {

    abstract val fileName: String

    data class Local(
        val file: File,
        override val version: String,
    ) : UpdateData(version) {

        override val fileName: String
            get() = file.name
    }

    data class Remote(
        val file: Blob,
        override val version: String,
    ) : UpdateData(version) {

        override val fileName: String
            get() = file.name
    }
}
