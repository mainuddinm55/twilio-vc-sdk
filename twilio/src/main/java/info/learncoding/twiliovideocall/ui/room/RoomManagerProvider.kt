package info.learncoding.twiliovideocall.ui.room

import info.learncoding.twiliovideocall.di.RoomComponent
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class RoomManagerProvider @Inject constructor(
    private val roomComponentProvider: Provider<RoomComponent.Builder>
) {

    var roomComponent: RoomComponent? = null

    fun createRoomScope(roomManager: RoomManager) {
        roomComponent = roomComponentProvider.get()
            .setRoomManager(roomManager).build()
    }

    fun destroyScope() {
        roomComponent = null
    }
}