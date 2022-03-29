package update

import kotlinx.coroutines.flow.Flow

interface UpdatesLoader {

    fun deleteLocalUpdateFiles()

    fun checkForUpdate(): Flow<UpdateAvailability>

    fun loadUpdate(): Flow<UpdateStatus>
}
