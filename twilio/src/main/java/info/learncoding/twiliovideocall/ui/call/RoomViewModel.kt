package info.learncoding.twiliovideocall.ui.call

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.learncoding.twiliovideocall.data.model.CallOptions
import info.learncoding.twiliovideocall.ui.room.RoomActionEvent
import info.learncoding.twiliovideocall.ui.room.RoomManager
import kotlinx.coroutines.launch

private const val TAG = "RoomViewModel"

class RoomViewModel constructor(
    private val roomManager: RoomManager,
) : ViewModel() {

    val callState = roomManager.callState
    val viewState = roomManager.viewState
    val duration = RoomManager.duration

    fun processInput(viewEvent: RoomActionEvent) {
        Log.d(TAG, "View Event: $viewEvent")

        when (viewEvent) {
            is RoomActionEvent.Setup -> {
                roomManager.setupLocalTrack(viewEvent.isPermissionGranted)
                roomManager.updateServiceUiState(false)

            }
            is RoomActionEvent.SwitchAudioDevice -> {
                roomManager.selectDevice(viewEvent.device)
            }
            is RoomActionEvent.Connect -> {
                connect(viewEvent.callOptions)
            }
            RoomActionEvent.ToggleLocalVideo -> roomManager.toggleLocalVideo()
            RoomActionEvent.ToggleLocalAudio -> roomManager.toggleLocalAudio()
            RoomActionEvent.SwitchCamera -> roomManager.switchCamera()
            is RoomActionEvent.VideoTrackRemoved -> {
                roomManager.updateParticipantVideoTrack(viewEvent.sid, null)
            }
            RoomActionEvent.Disconnect -> roomManager.disconnect()
        }
    }

    private fun connect(callOptions: CallOptions) =
        viewModelScope.launch {
            roomManager.connect(callOptions)
        }

    fun showFloatingWidget() {
        roomManager.updateServiceUiState(true)
    }

    class Factory(private val roomManager: RoomManager) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoomViewModel(roomManager) as T
        }
    }
}
