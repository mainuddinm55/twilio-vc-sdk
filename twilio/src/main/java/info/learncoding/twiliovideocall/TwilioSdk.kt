package info.learncoding.twiliovideocall

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat


object TwilioSdk {
    const val REQUEST_CODE_ANSWER = 33
    const val REQUEST_CODE_END = 34
    const val REQUEST_CODE_CALL_END = 35

    const val EXTRA_TYPE = "TYPE"
    const val EXTRA_MSG = "msg"
    const val EXTRA_CALL_OPTIONS = "options"
    const val EXTRA_CALL_DATA_KEY = "key"

    const val ACTION_CALLBACK = "ACTION_VIDEO_CALL_CALLBACK"
    const val ACTION_CALL = "ACTION_CALL"
    const val ACTION_CALL_DATA = "ACTION_CALL_DATA"

    const val TYPE_REJECT = "REJECT"
    const val TYPE_RINGING = "RINGING"
    const val TYPE_MISSED_CALL = "MISSED_CALL"
    const val TYPE_INCOMING = "INCOMING"
    const val TYPE_ACCEPT = "ACCEPT"
    const val TYPE_END = "END"
    const val TYPE_FAILED = "FAILED"
    const val TYPE_OUTGOING = "OUTGOING"
    const val TYPE_CONNECTING = "CONNECTING"
    const val TYPE_CONNECTED = "CONNECTED"
    const val TYPE_RECONNECTING = "RECONNECTING"
    const val TYPE_RECONNECTED = "RECONNECTED"

    const val INCOMING_NOTIFICATION_CHANNEL_ID = "Incoming Call"
    const val ONGOING_NOTIFICATION_CHANNEL_ID = "Ongoing Call"

    const val KEY_VIDEO_CALL_DROP = "video_call_drop_"
    const val KEY_VIDEO_CALL_MISSED_CALL = "video_call_missed_call_"
    const val KEY_VIDEO_CALL_BUTTON_STATE = "video_call_button_states_"
    const val KEY_VIDEO_CALL_RECEIVING_TIME = "video_call_receiving_time_"
    const val KEY_VIDEO_CALL_CALLING_TIME = "video_call_calling_time_"
    const val KEY_VIDEO_CALL_NETWORK_QUALITY = "video_call_net_quality_"
    const val KEY_VIDEO_CALL_DOMINANT_SPEAKER = "video_call_dominant_speaker_"
    const val KEY_VIDEO_CALL_TWILIO_EXCEPTION = "video_call_twilio_exception_"

    fun initSdk(context: Context) {
        createNotificationChannel(context)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INCOMING_NOTIFICATION_CHANNEL_ID,
                INCOMING_NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Call Notification"
                setShowBadge(true)
                setSound(
                    null, null/*
                    Uri.parse("android.resource://" + context.packageName + "/" + R.raw.twilio_incoming_ringtone),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()*/
                )
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val silentChannel = NotificationChannel(
                ONGOING_NOTIFICATION_CHANNEL_ID,
                ONGOING_NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Call Notification"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }

    fun networkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_CELLULAR
                    ) -> "DATA"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    else -> "Unknown"
                }
            } else {
                "Unknown"
            }
        } else {
            val networkInfo = cm.activeNetworkInfo
            if (networkInfo != null) {
                if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) "DATA" else if (networkInfo.type == ConnectivityManager.TYPE_WIFI) "WIFI" else if (networkInfo.type == ConnectivityManager.TYPE_VPN) "VPN" else "Unknown"
            } else {
                "Unknown"
            }
        }
    }
}