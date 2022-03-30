package update

import kotlinx.coroutines.flow.StateFlow

interface UpdatesLoader {

    val updateAvailabilityFlow: StateFlow<UpdateAvailability>

    fun deleteLocalUpdateFiles()

    fun checkForUpdate()

    fun loadUpdate(remoteUpdateData: UpdateData.Remote)
}
