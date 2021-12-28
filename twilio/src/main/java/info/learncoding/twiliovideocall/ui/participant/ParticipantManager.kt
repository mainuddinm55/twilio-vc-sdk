package info.learncoding.twiliovideocall.ui.participant

import android.util.Log
import com.twilio.video.NetworkQualityLevel
import com.twilio.video.TrackPriority.HIGH
import info.learncoding.twiliovideocall.ui.room.VideoTrackViewState

private const val TAG = "ParticipantManager"

class ParticipantManager {
    private val mutableParticipants = mutableListOf<ParticipantViewState>()
    val participantThumbnails: List<ParticipantViewState>
        get() {
            return if (mutableParticipants.size > 1) {
                mutableParticipants.filter { it.isLocalParticipant }
            } else {
                emptyList()
            }
        }
    var primaryParticipant: ParticipantViewState
        private set

    init {
        val localParticipant = ParticipantViewState(isLocalParticipant = true)
        mutableParticipants.add(localParticipant)
        primaryParticipant = localParticipant
    }

    fun addParticipant(participantViewState: ParticipantViewState) {
        Log.d(TAG, "Adding participant: $participantViewState")
        mutableParticipants.add(participantViewState)
        updatePrimaryParticipant()
    }

    fun updateLocalParticipantVideoTrack(videoTrack: VideoTrackViewState?) =
        mutableParticipants.find { it.isLocalParticipant }?.copy(
            videoTrack = videoTrack
        )?.let { updateLocalParticipant(it) }

    fun updateLocalParticipantSid(sid: String) =
        mutableParticipants.find { it.isLocalParticipant }?.copy(
            sid = sid
        )?.let { updateLocalParticipant(it) }

    private fun updateParticipant(
        participantViewState: ParticipantViewState,
        participantMatchPredicate: (ParticipantViewState) -> Boolean = {
            it.sid == participantViewState.sid
        }
    ) {

        mutableParticipants.indexOfFirst(participantMatchPredicate).let { index ->
            if (index > -1) {
                Log.d(TAG, "Updating participant: $participantViewState")
                mutableParticipants[index] = participantViewState
                updatePrimaryParticipant()
            }
        }
    }

    fun removeParticipant(sid: String) {
        Log.d(TAG, "Removing participant: $sid")
        mutableParticipants.removeAll { it.sid == sid }
        updatePrimaryParticipant()
    }

    private fun getParticipant(sid: String): ParticipantViewState? =
        mutableParticipants.find { it.sid == sid }

    fun updateNetworkQuality(sid: String, networkQualityLevel: NetworkQualityLevel) {
        getParticipant(sid)?.copy(networkQualityLevel = networkQualityLevel)?.let {
            updateParticipant(it)
        }
    }

    fun updateParticipantVideoTrack(sid: String, videoTrack: VideoTrackViewState?) {
        mutableParticipants.find { it.sid == sid }?.copy(
            videoTrack = videoTrack
        )?.let { updateParticipant(it) }
    }

    fun muteParticipant(sid: String, mute: Boolean) {
        getParticipant(sid)?.copy(isMuted = mute)?.let {
            updateParticipant(it)
        }
    }

    private fun updateLocalParticipant(participantViewState: ParticipantViewState) =
        updateParticipant(participantViewState) { it.isLocalParticipant }

    fun clearRemoteParticipants() {
        mutableParticipants.removeAll { !it.isLocalParticipant }
        updatePrimaryParticipant()
    }

    private fun updatePrimaryParticipant() {
        primaryParticipant = retrievePrimaryParticipant()
        Log.d(TAG, "Participant Cache: $mutableParticipants")
        Log.d(TAG, "Primary Participant: $primaryParticipant")
    }

    private fun retrievePrimaryParticipant(): ParticipantViewState =
        determinePrimaryParticipant().apply { setTrackPriority(this) }

    private fun determinePrimaryParticipant(): ParticipantViewState {
        return mutableParticipants.find { !it.isLocalParticipant }
            ?: mutableParticipants.find { it.isLocalParticipant }
            ?: mutableParticipants[0] // local participant
    }

    private fun setTrackPriority(participant: ParticipantViewState) {
        if (participant.sid != primaryParticipant.sid) {
            participant.getRemoteVideoTrack()?.let {
                it.priority = HIGH
                clearOldTrackPriorities()
                Log.d(
                    TAG,
                    "Setting video track priority to high for participant with sid: ${participant.sid}"
                )
            }
        }
        if (participant.isLocalParticipant) clearOldTrackPriorities()
    }

    private fun clearOldTrackPriorities() {
        primaryParticipant.run {
            getRemoteVideoTrack()?.priority = null
            Log.d(TAG, "Clearing video and screen track priorities for participant with sid: $sid")
        }
    }

}
