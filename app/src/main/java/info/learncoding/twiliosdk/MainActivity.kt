package info.learncoding.twiliosdk

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
//import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.android.material.button.MaterialButton
//import com.google.firebase.messaging.FirebaseMessaging
//import com.google.firebase.messaging.FirebaseMessagingService
import dagger.hilt.android.AndroidEntryPoint
import info.learncoding.twiliovideocall.data.model.CallOptions
import info.learncoding.twiliovideocall.data.model.UserType
import info.learncoding.twiliovideocall.service.VideoCallService
import info.learncoding.twiliovideocall.ui.call.OnGoingCallActivity
import info.learncoding.twiliovideocall.ui.participant.ParticipantViewState
import info.learncoding.twiliovideocall.ui.room.CallState
import info.learncoding.twiliovideocall.ui.room.RoomViewState
import info.learncoding.twiliovideocall.utils.serializeToMap

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val baseUrl = BuildConfig.BASE_URL
        val room = BuildConfig.ROOM
        val caller = BuildConfig.CALLER
        val receiver = BuildConfig.RECEIVER
        findViewById<MaterialButton>(R.id.send_call).setOnClickListener {
            VideoCallService.startService(
                this,
                CallOptions(
                    "$baseUrl/$room/$caller",
                    room,
                    caller,
                    null,
                    "Md Mainuddin",
                    listOf("https://upload.wikimedia.org/wikipedia/commons/thumb/b/b6/Image_created_with_a_mobile_phone.png/440px-Image_created_with_a_mobile_phone.png"),
                    UserType.CALLER
                ),
                VideoCallListenerReceiver()
            )
        }

        findViewById<MaterialButton>(R.id.receive_call).setOnClickListener {
            VideoCallService.startService(
                this,
                CallOptions(
                    "$baseUrl/$room/$receiver",
                    room,
                    receiver,
                    null,
                    "Anup",
                    emptyList(),
                    UserType.RECEIVER
                ),
                VideoCallListenerReceiver()
            )
        }

        /*FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.d(this::class.java.name, "onCreate FcmToken: $it")
        }*/


        val callState = CallState.Lobby
        Log.d(TAG, "onCreate: ${callState.javaClass.simpleName}")

        val state = RoomViewState(
            primaryParticipant = ParticipantViewState(isLocalParticipant = true),
            participantThumbnails = listOf(ParticipantViewState()),

            )
        Log.d(TAG, "onCreate: ${state.serializeToMap()}")
    }


}