package update

sealed class UpdateAvailability {

    data class Error(
        val cause: Throwable,
    ) : UpdateAvailability()

    object NoUpdate : UpdateAvailability()

    sealed class HasUpdate(open val updateData: UpdateData) : UpdateAvailability() {

        data class ReadyToDownload(override val updateData: UpdateData.Remote) : HasUpdate(updateData)

        data class Downloading(override val updateData: UpdateData.Remote, val downloaded: Int) : HasUpdate(updateData)

        data class DownloadError(override val updateData: UpdateData.Remote, val cause: Throwable) : HasUpdate(updateData)

        data class ReadyToUpdate(override val updateData: UpdateData.Local) : HasUpdate(updateData)
    }
}
