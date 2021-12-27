package info.learncoding.twiliovideocall

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
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

    const val ACTION_CALLBACK = "ACTION_VIDEO_CALL_CALLBACK"
    const val ACTION_CALL = "ACTION_CALL"

    const val TYPE_REJECT = "REJECT"
    const val TYPE_RINGING = "RINGING"
    const val TYPE_MISSED_CALL = "MISSED_CALL"
    const val TYPE_INCOMING = "INCOMING"
    const val TYPE_ACCEPT = "ACCEPT"
    const val TYPE_END = "END"
    const val TYPE_FAILED = "FAILED"
    const val TYPE_CONNECTING = "CONNECTING"
    const val TYPE_CONNECTED = "CONNECTED"
    const val TYPE_RECONNECTING = "RECONNECTING"
    const val TYPE_RECONNECTED = "RECONNECTED"

    const val INCOMING_NOTIFICATION_CHANNEL_ID = "Incoming Calls"
    const val ONGOING_NOTIFICATION_CHANNEL_ID = "Ongoing Calls"

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
                    Uri.parse("android.resource://" + context.packageName + "/" + R.raw.twilio_incoming_ringtone),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                )
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val silentChannel = NotificationChannel(
                ONGOING_NOTIFICATION_CHANNEL_ID,
                ONGOING_NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Call Notification"
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }
}