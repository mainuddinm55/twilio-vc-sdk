package info.learncoding.twiliovideocall.ui.participant

import android.util.Log
import com.twilio.video.*
import info.learncoding.twiliovideocall.ui.room.RoomManager

private const val TAG = "LocalParticipantListene"

class LocalParticipantListener(private val roomManager: RoomManager) : LocalParticipant.Listener {

    override fun onNetworkQualityLevelChanged(
        localParticipant: LocalParticipant,
        networkQualityLevel: NetworkQualityLevel
    ) {
        Log.i(
            TAG,
            "LocalParticipant NetworkQualityLevel changed for LocalParticipant sid: ${localParticipant.sid}, NetworkQualityLevel: $networkQualityLevel"
        )
        roomManager.updateParticipantNetworkQualityChanged(
            localParticipant.sid, networkQualityLevel
        )
    }

    override fun onVideoTrackPublished(
        localParticipant: LocalParticipant,
        localVideoTrackPublication: LocalVideoTrackPublication
    ) {
        Log.i(
            TAG,
            "onVideoTrackPublished: ${localParticipant.sid} local video track ${localVideoTrackPublication.localVideoTrack}"
        )
    }

    override fun onVideoTrackPublicationFailed(
        localParticipant: LocalParticipant,
        localVideoTrack: LocalVideoTrack,
        twilioException: TwilioException
    ) {
        Log.i(
            TAG,
            "onVideoTrackPublicationFailed: ${localParticipant.sid} local video track $localParticipant",
            twilioException
        )
    }

    override fun onDataTrackPublished(
        localParticipant: LocalParticipant,
        localDataTrackPublication: LocalDataTrackPublication
    ) {
        Log.i(
            TAG,
            "onDataTrackPublished: ${localParticipant.sid} local data track ${localDataTrackPublication.localDataTrack}"
        )
    }

    override fun onDataTrackPublicationFailed(
        localParticipant: LocalParticipant,
        localDataTrack: LocalDataTrack,
        twilioException: TwilioException
    ) {
        Log.i(
            TAG,
            "onDataTrackPublicationFailed: ${localParticipant.sid} local data track $localDataTrack",
            twilioException
        )
    }

    override fun onAudioTrackPublished(
        localParticipant: LocalParticipant,
        localAudioTrackPublication: LocalAudioTrackPublication
    ) {
        Log.i(
            TAG,
            "onAudioTrackPublished: ${localParticipant.sid} local audio track ${localAudioTrackPublication.localAudioTrack}"
        )
    }

    override fun onAudioTrackPublicationFailed(
        localParticipant: LocalParticipant,
        localAudioTrack: LocalAudioTrack,
        twilioException: TwilioException
    ) {
        Log.i(
            TAG,
            "onAudioTrackPublicationFailed: ${localParticipant.sid} local audio track $localAudioTrack",
            twilioException
        )
    }
}
