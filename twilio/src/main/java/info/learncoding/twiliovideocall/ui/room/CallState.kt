package info.learncoding.twiliovideocall.ui.room

import com.twilio.video.Room
import info.learncoding.twiliovideocall.data.model.CallOptions
import io.uniflow.core.flow.data.UIEvent

sealed class CallState(val options: CallOptions?) {
    object Lobby : CallState(null)

    data class Connecting(val callOptions: CallOptions?) : CallState(callOptions)
    data class Connected(
        val room: Room, val callOptions: CallOptions?
    ) : CallState(callOptions)

    data class Reconnecting(val callOptions: CallOptions?) : CallState(callOptions)
    data class Disconnected(val isReject: Boolean) : CallState(null)
    data class Incoming(val callOptions: CallOptions) : CallState(callOptions)
    data class Ringing(val callOptions: CallOptions?) : CallState(callOptions)
    data class ConnectionFailed(val msg: String? = null) : CallState(null)
}
