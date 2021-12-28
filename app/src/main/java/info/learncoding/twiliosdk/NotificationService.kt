package info.learncoding.twiliosdk

import android.util.Log
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
import info.learncoding.twiliovideocall.data.model.CallOptions
import info.learncoding.twiliovideocall.data.model.UserType
import info.learncoding.twiliovideocall.service.VideoCallService

/*
class NotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(this::class.java.name, "onMessageReceived: ${remoteMessage.data}")
        if (remoteMessage.data["action"] == "ACTION_CALL") {
            showCallNotification(remoteMessage.data)
        }
    }

    private fun showCallNotification(data: Map<String, String>) {
        val type = data["type"]
        val room = data["room_name"]
        val userId = data["user_id_to"]
        val remoteUserId = data["user_id_from"]
        val url = "${BuildConfig.BASE_URL}/$room/$userId"
        when (type) {
            "INCOMING" -> {
                VideoCallService.startService(
                    this,
                    CallOptions(
                        url,
                        room ?: "",
                        remoteUserId ?: "",
                        data["schedule_id"],
                        remoteUserId ?: "",
                        emptyList(),
                        UserType.RECEIVER
                    ),
                    VideoCallListenerReceiver()
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(this::class.java.name, "onNewToken: $token")
    }
}*/
