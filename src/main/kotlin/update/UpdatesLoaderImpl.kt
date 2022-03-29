package update

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import di.AppParameters
import di.applicationDataDirectory
import di.version
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class UpdatesLoaderImpl(
    private val appParameters: AppParameters,
    private val firebaseBucket: Bucket,
) : UpdatesLoader, CoroutineScope {

    override val coroutineContext: CoroutineContext by lazy {
        Dispatchers.Default + SupervisorJob()
    }

    private val updateFilesRegex = Regex("announcer-(?<version>\\d+\\.\\d+\\.\\d+)\\.[a-z0-9]+")

    override fun deleteLocalUpdateFiles() {
        launch(Dispatchers.IO) {
            val currentVersion = appParameters.version
            val files = applicationDataDirectory.listFiles { _, name ->
                val matches = updateFilesRegex.matchEntire(name) ?: return@listFiles false
                if (currentVersion < matches.groups["version"]!!.value) return@listFiles false
                true
            } ?: emptyArray()
            println("Found files: ${files.joinToString { it.name }}")
            val deleted = files.count { it.delete() }
            println("Deleted files: $deleted")
        }
    }

    override fun checkForUpdate(): Flow<UpdateAvailability> {
        return flow {
            val currentVersion = appParameters.version
            emit(UpdateAvailability.Checking)
            val localFiles = applicationDataDirectory.listFiles { _, name -> name.matches(updateFilesRegex) } ?: emptyArray()
            val localFileForUpdate = localFiles
                .associateWith { updateFilesRegex.matchEntire(it.name)?.groups?.get("version")?.value }
                .mapNotNull { if (it.value == null) null else it.key to it.value!! }
                .maxByOrNull { (_, version) -> version }
                ?.let { if (it.second > currentVersion) it else null }
            try {
                val updateFile = firebaseBucket.list(Storage.BlobListOption.currentDirectory()).values
                    .associateWith { updateFilesRegex.matchEntire(it.name)?.groups?.get("version")?.value }
                    .mapNotNull { if (it.value == null) null else it.key to it.value!! }
                    .maxByOrNull { (_, version) -> version }
                    ?.let { if (it.second > currentVersion) it else null }
                val status = when {
                    localFileForUpdate != null && updateFile != null -> when {
                        currentVersion >= localFileForUpdate.second && currentVersion >= updateFile.second -> UpdateAvailability.NoUpdate
                        localFileForUpdate.second >= updateFile.second -> UpdateAvailability.HasUpdate.Local(localFileForUpdate.first, localFileForUpdate.second)
                        else -> UpdateAvailability.HasUpdate.Remote(updateFile.first, updateFile.second)
                    }
                    updateFile != null -> when {
                        currentVersion >= updateFile.second -> UpdateAvailability.NoUpdate
                        else -> UpdateAvailability.HasUpdate.Remote(updateFile.first, updateFile.second)
                    }
                    localFileForUpdate != null -> when {
                        currentVersion >= localFileForUpdate.second -> UpdateAvailability.NoUpdate
                        else -> UpdateAvailability.HasUpdate.Local(localFileForUpdate.first, localFileForUpdate.second)
                    }
                    else -> UpdateAvailability.NoUpdate
                }
                emit(status)
            } catch (e: Exception) {
                e.printStackTrace()
                emit(UpdateAvailability.Error(e))
            }
        }
    }

    override fun loadUpdate(): Flow<UpdateStatus> {
        return flow { UpdateStatus.Error(Exception("Not implemented")) }
    }
}
