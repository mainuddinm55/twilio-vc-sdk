package info.learncoding.twiliovideocall.ui.room

import com.twilio.audioswitch.AudioDevice
import info.learncoding.twiliovideocall.data.model.CallOptions

sealed class RoomActionEvent {
    data class Setup(val isPermissionGranted: Boolean = false) : RoomActionEvent()
    object ToggleLocalVideo : RoomActionEvent()
    object ToggleLocalAudio : RoomActionEvent()
    object SwitchCamera : RoomActionEvent()
    data class SwitchAudioDevice(val device: AudioDevice) : RoomActionEvent()
    data class Connect(val callOptions: CallOptions) : RoomActionEvent()
    data class VideoTrackRemoved(val sid: String) : RoomActionEvent()
    object Disconnect : RoomActionEvent()
}
