package update

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import di.AppParameters
import di.version
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import utils.AppDataFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

class UpdatesLoaderImpl(
    private val appParameters: AppParameters,
    private val firebaseBucket: Bucket,
) : UpdatesLoader, CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        Dispatchers.Default + SupervisorJob()
    }

    private val updateFilesRegex = Regex("announcer-(?<version>\\d+\\.\\d+\\.\\d+)\\.[a-z0-9]+")

    private val _updateAvailabilityFlow = MutableStateFlow<UpdateAvailability>(UpdateAvailability.NoUpdate)
    override val updateAvailabilityFlow: StateFlow<UpdateAvailability>
        get() = _updateAvailabilityFlow

    override fun deleteLocalUpdateFiles() {
        launch(Dispatchers.IO) {
            val currentVersion = appParameters.version
            val files = AppDataFolder.listFiles { _, name ->
                val matches = updateFilesRegex.matchEntire(name) ?: return@listFiles false
                if (currentVersion < matches.groups["version"]!!.value) return@listFiles false
                true
            } ?: emptyArray()
            println("Found files: ${files.joinToString { it.name }}")
            val deleted = files.count { it.delete() }
            println("Deleted files: $deleted")
        }
    }

    private suspend fun getLocalUpdateFile(currentVersion: String): UpdateData.Local? {
        return withContext(Dispatchers.IO) {
            val localFiles = AppDataFolder.listFiles { _, name -> name.matches(updateFilesRegex) } ?: emptyArray()
            localFiles
                .mapNotNull {
                    val version = updateFilesRegex.matchEntire(it.name)
                        ?.groups?.get("version")?.value
                        ?: return@mapNotNull null
                    UpdateData.Local(it, version)
                }
                .maxByOrNull { it.version }
                ?.let { if (it.version > currentVersion) it else null }
        }
    }

    private suspend fun getRemoteUpdateFile(currentVersion: String): UpdateData.Remote? {
        return withContext(Dispatchers.IO) {
            firebaseBucket.list(Storage.BlobListOption.currentDirectory()).values
                .mapNotNull {
                    val version = updateFilesRegex.matchEntire(it.name)
                        ?.groups?.get("version")?.value
                        ?: return@mapNotNull null
                    UpdateData.Remote(it, version)
                }
                .maxByOrNull { it.version }
                ?.let { if (it.version > currentVersion) it else null }
        }
    }

    override fun checkForUpdate() {
        launch {
            val currentVersion = appParameters.version
            try {
                val localFile = getLocalUpdateFile(currentVersion)
                val remoteFile = getRemoteUpdateFile(currentVersion)
                val status = when {
                    localFile != null && remoteFile != null -> when {
                        currentVersion >= localFile.version && currentVersion >= remoteFile.version -> UpdateAvailability.NoUpdate
                        localFile.version >= remoteFile.version -> UpdateAvailability.HasUpdate.ReadyToUpdate(localFile)
                        else -> UpdateAvailability.HasUpdate.ReadyToDownload(remoteFile)
                    }
                    remoteFile != null -> when {
                        currentVersion >= remoteFile.version -> UpdateAvailability.NoUpdate
                        else -> UpdateAvailability.HasUpdate.ReadyToDownload(remoteFile)
                    }
                    localFile != null -> when {
                        currentVersion >= localFile.version -> UpdateAvailability.NoUpdate
                        else -> UpdateAvailability.HasUpdate.ReadyToUpdate(localFile)
                    }
                    else -> UpdateAvailability.NoUpdate
                }
                _updateAvailabilityFlow.value = status
            } catch (e: Exception) {
                e.printStackTrace()
                _updateAvailabilityFlow.value = UpdateAvailability.Error(e)
            }
        }
    }

    override fun loadUpdate(remoteUpdateData: UpdateData.Remote) {
        launch(Dispatchers.IO) {
            _updateAvailabilityFlow.value = UpdateAvailability.HasUpdate.Downloading(remoteUpdateData, 0L)
            val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
            val targetFile = File(AppDataFolder, remoteUpdateData.fileName).also { it.createNewFile() }
            val writer = FileOutputStream(targetFile)
            val reader = firebaseBucket.storage.reader(
                remoteUpdateData.file.blobId,
                Storage.BlobSourceOption.shouldReturnRawInputStream(true)
            )
            try {
                var totalWritten = 0L
                var read: Int
                while (reader.read(buffer).also { read = it } >= 0) {
                    writer.write(buffer.array().copyOf(read))
                    totalWritten += read
                    _updateAvailabilityFlow.value = UpdateAvailability.HasUpdate.Downloading(remoteUpdateData, totalWritten)
                    read = 0
                    buffer.clear()
                }
                if (totalWritten == remoteUpdateData.file.size) {
                    _updateAvailabilityFlow.value = UpdateAvailability.HasUpdate.ReadyToUpdate(UpdateData.Local(targetFile, remoteUpdateData.version))
                } else {
                    _updateAvailabilityFlow.value = UpdateAvailability.HasUpdate.DownloadError(
                        remoteUpdateData,
                        IllegalStateException("Size control failure. Required: ${remoteUpdateData.file.size}, given: $totalWritten")
                    )
                    targetFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                targetFile.delete()
                _updateAvailabilityFlow.value = UpdateAvailability.HasUpdate.DownloadError(remoteUpdateData, e)
            } finally {
                buffer.clear()
                writer.close()
                reader.close()
            }
        }
    }
}
