package info.learncoding.twiliovideocall.ui.participant

import android.util.Log
import com.twilio.video.*
import info.learncoding.twiliovideocall.ui.room.RoomManager
import info.learncoding.twiliovideocall.ui.room.VideoTrackViewState

private const val TAG = "RemoteParticipantListen"

class RemoteParticipantListener(private val roomManager: RoomManager) : RemoteParticipant.Listener {

    override fun onVideoTrackSwitchedOff(
        remoteParticipant: RemoteParticipant,
        remoteVideoTrack: RemoteVideoTrack
    ) {
        Log.i(
            TAG,
            "RemoteVideoTrack switched off for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteVideoTrack sid: ${remoteVideoTrack.sid}"
        )
        roomManager.updateParticipantVideoTrack(
            remoteParticipant.sid,
            VideoTrackViewState(
                remoteVideoTrack,
                isSwitchedOff = true
            )
        )

    }

    override fun onVideoTrackSwitchedOn(
        remoteParticipant: RemoteParticipant,
        remoteVideoTrack: RemoteVideoTrack
    ) {
        Log.i(
            TAG,
            "RemoteVideoTrack switched on for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteVideoTrack sid: ${remoteVideoTrack.sid}"
        )

        roomManager.updateParticipantVideoTrack(
            remoteParticipant.sid,
            VideoTrackViewState(
                remoteVideoTrack,
                isSwitchedOff = false
            )
        )
    }

    override fun onVideoTrackSubscribed(
        remoteParticipant: RemoteParticipant,
        remoteVideoTrackPublication: RemoteVideoTrackPublication,
        remoteVideoTrack: RemoteVideoTrack
    ) {
        Log.i(
            TAG,
            "RemoteVideoTrack subscribed for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteVideoTrack sid: ${remoteVideoTrack.sid}"
        )
        roomManager.updateParticipantVideoTrack(
            remoteParticipant.sid,
            VideoTrackViewState(remoteVideoTrack)
        )
    }

    override fun onVideoTrackUnsubscribed(
        remoteParticipant: RemoteParticipant,
        remoteVideoTrackPublication: RemoteVideoTrackPublication,
        remoteVideoTrack: RemoteVideoTrack
    ) {
        Log.i(
            TAG,
            "RemoteVideoTrack unsubscribed for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteVideoTrack sid: ${remoteVideoTrack.sid}"
        )
        roomManager.updateParticipantVideoTrack(
            remoteParticipant.sid, null
        )
    }

    override fun onNetworkQualityLevelChanged(
        remoteParticipant: RemoteParticipant,
        networkQualityLevel: NetworkQualityLevel
    ) {
        Log.i(
            TAG,
            "RemoteParticipant NetworkQualityLevel changed for RemoteParticipant sid: ${remoteParticipant.sid}, NetworkQualityLevel: $networkQualityLevel"
        )
        roomManager.updateParticipantNetworkQualityChanged(
            remoteParticipant.sid,
            networkQualityLevel
        )
    }

    override fun onAudioTrackSubscribed(
        remoteParticipant: RemoteParticipant,
        remoteAudioTrackPublication: RemoteAudioTrackPublication,
        remoteAudioTrack: RemoteAudioTrack
    ) {
        Log.i(
            TAG,
            "RemoteParticipant AudioTrack subscribed for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteAudioTrack sid: ${remoteAudioTrack.sid}"
        )
        roomManager.updateParticipantAudioTrack(remoteAudioTrack.sid, false)
    }

    override fun onAudioTrackUnsubscribed(
        remoteParticipant: RemoteParticipant,
        remoteAudioTrackPublication: RemoteAudioTrackPublication,
        remoteAudioTrack: RemoteAudioTrack
    ) {
        Log.i(
            TAG,
            "RemoteParticipant AudioTrack unsubscribed for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteAudioTrack sid: ${remoteAudioTrack.sid}"
        )
        roomManager.updateParticipantAudioTrack(remoteAudioTrack.sid, true)
    }

    override fun onAudioTrackPublished(
        remoteParticipant: RemoteParticipant,
        remoteAudioTrackPublication: RemoteAudioTrackPublication
    ) {
        Log.i(
            TAG,
            "RemoteParticipant AudioTrack published for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteAudioTrack sid: ${remoteAudioTrackPublication.trackSid}"
        )
        roomManager.updateParticipantAudioTrack(remoteParticipant.sid, false)
    }

    override fun onAudioTrackUnpublished(
        remoteParticipant: RemoteParticipant,
        remoteAudioTrackPublication: RemoteAudioTrackPublication
    ) {
        Log.i(
            TAG,
            "RemoteParticipant AudioTrack unpublished for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteAudioTrack sid: ${remoteAudioTrackPublication.trackSid}"
        )
        roomManager.updateParticipantAudioTrack(remoteParticipant.sid, true)
    }

    override fun onAudioTrackEnabled(
        remoteParticipant: RemoteParticipant,
        remoteAudioTrackPublication: RemoteAudioTrackPublication
    ) {
        Log.i(
            TAG,
            "RemoteParticipant AudioTrack enabled for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteAudioTrack sid: ${remoteAudioTrackPublication.trackSid}"
        )
        roomManager.updateParticipantAudioTrack(remoteParticipant.sid, false)
    }

    override fun onAudioTrackDisabled(
        remoteParticipant: RemoteParticipant,
        remoteAudioTrackPublication: RemoteAudioTrackPublication
    ) {
        Log.i(
            TAG,
            "RemoteParticipant AudioTrack disabled for RemoteParticipant sid: ${remoteParticipant.sid}, RemoteAudioTrack sid: ${remoteAudioTrackPublication.trackSid}"
        )
        roomManager.updateParticipantAudioTrack(remoteParticipant.sid, true)
    }

    override fun onDataTrackPublished(
        remoteParticipant: RemoteParticipant,
        remoteDataTrackPublication: RemoteDataTrackPublication
    ) {
        Log.i(
            TAG,
            "onDataTrackPublished: ${remoteParticipant.sid} remote data track ${remoteDataTrackPublication.remoteDataTrack}"
        )
    }

    override fun onVideoTrackPublished(
        remoteParticipant: RemoteParticipant,
        remoteVideoTrackPublication: RemoteVideoTrackPublication
    ) {
        Log.i(
            TAG,
            "onVideoTrackPublished: ${remoteParticipant.sid} remote video track ${remoteVideoTrackPublication.remoteVideoTrack}"
        )
    }

    override fun onVideoTrackEnabled(
        remoteParticipant: RemoteParticipant,
        remoteVideoTrackPublication: RemoteVideoTrackPublication
    ) {
        Log.i(
            TAG,
            "onVideoTrackEnabled: ${remoteParticipant.sid} remote video track ${remoteVideoTrackPublication.remoteVideoTrack}"
        )
    }

    override fun onVideoTrackDisabled(
        remoteParticipant: RemoteParticipant,
        remoteVideoTrackPublication: RemoteVideoTrackPublication
    ) {
        Log.i(
            TAG,
            "onVideoTrackDisabled: ${remoteParticipant.sid} remote video track ${remoteVideoTrackPublication.remoteVideoTrack}"
        )
    }

    override fun onDataTrackSubscriptionFailed(
        remoteParticipant: RemoteParticipant,
        remoteDataTrackPublication: RemoteDataTrackPublication,
        twilioException: TwilioException
    ) {
    }

    override fun onDataTrackSubscribed(
        remoteParticipant: RemoteParticipant,
        remoteDataTrackPublication: RemoteDataTrackPublication,
        remoteDataTrack: RemoteDataTrack
    ) {
    }

    override fun onVideoTrackSubscriptionFailed(
        remoteParticipant: RemoteParticipant,
        remoteVideoTrackPublication: RemoteVideoTrackPublication,
        twilioException: TwilioException
    ) {
    }

    override fun onAudioTrackSubscriptionFailed(
        remoteParticipant: RemoteParticipant,
        remoteAudioTrackPublication: RemoteAudioTrackPublication,
        twilioException: TwilioException
    ) {
    }

    override fun onVideoTrackUnpublished(
        remoteParticipant: RemoteParticipant,
        remoteVideoTrackPublication: RemoteVideoTrackPublication
    ) {
    }

    override fun onDataTrackUnsubscribed(
        remoteParticipant: RemoteParticipant,
        remoteDataTrackPublication: RemoteDataTrackPublication,
        remoteDataTrack: RemoteDataTrack
    ) {
    }

    override fun onDataTrackUnpublished(
        remoteParticipant: RemoteParticipant,
        remoteDataTrackPublication: RemoteDataTrackPublication
    ) {
    }
}
