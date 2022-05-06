package di

import kotlinx.coroutines.flow.MutableSharedFlow

typealias MessageSource = String

typealias Content = String

typealias MessagesFlow = MutableSharedFlow<Pair<MessageSource, Content>>

typealias AppStopper = () -> Unit

typealias AppParameters = Map<String, String>
val AppParameters.version: String
    get() = this["version"]!!
val AppParameters.encryptionKey: String
    get() = this["encryptionKey"]!!
val AppParameters.firebaseBucket: String
    get() = this["firebaseBucket"]!!
