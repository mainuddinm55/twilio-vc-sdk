package info.learncoding.twiliovideocall.utils

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import info.learncoding.twiliovideocall.R
import info.learncoding.twiliovideocall.TwilioSdk.INCOMING_NOTIFICATION_CHANNEL_ID
import info.learncoding.twiliovideocall.TwilioSdk.ONGOING_NOTIFICATION_CHANNEL_ID
import info.learncoding.twiliovideocall.TwilioSdk
import info.learncoding.twiliovideocall.data.model.CallOptions
import info.learncoding.twiliovideocall.receiver.VideoCallReceiver
import info.learncoding.twiliovideocall.ui.call.OnGoingCallActivity

object NotificationHelper {

    fun showNotification(context: Context, notificationId: Int, notification: Notification) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    fun buildCallNotification(
        context: Context,
        callOptions: CallOptions?,
        isOngoing: Boolean = false,
        showTimer: Boolean = false
    ): Notification {
        return if (isOngoing) {
            buildOngoingCallNotification(context, showTimer)
        } else {
            buildIncomingNotification(context, callOptions)
        }
    }

    fun buildOngoingCallNotification(context: Context, showTimer: Boolean): Notification {
        return NotificationCompat.Builder(context, ONGOING_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.twilio_ongoing_call_notification_title))
            .setContentText(context.getString(R.string.twilio_room_notification_message))
            .setSound(null)
            .apply {
                if (showTimer) {
                    addAction(
                        R.drawable.twilio_ic_baseline_call_end_24,
                        "End",
                        PendingIntent.getBroadcast(
                            context, TwilioSdk.REQUEST_CODE_CALL_END,
                            Intent(context, VideoCallReceiver::class.java).apply {
                                action = TwilioSdk.ACTION_CALL
                                putExtra(TwilioSdk.EXTRA_TYPE, TwilioSdk.TYPE_END)
                            },
                            getNotificationFlag()
                        )
                    )
                }
            }
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    43,
                    Intent(context, OnGoingCallActivity::class.java),
                    getNotificationFlag()
                ),
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setChronometerCountDown(showTimer)
                }
            }
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(showTimer)
            .setSmallIcon(R.drawable.twilio_ic_videocam_notification)
            .setTicker(context.getString(R.string.twilio_room_notification_message))
            .build()
    }


    private fun buildIncomingNotification(
        context: Context,
        callOptions: CallOptions?
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isAppBackground(context)) {
                val fullScreenIntent = Intent(context, OnGoingCallActivity::class.java)
                fullScreenIntent.flags = Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    3,
                    fullScreenIntent,
                    getNotificationFlag()
                )

                val acceptIntent = Intent(context, VideoCallReceiver::class.java)
                acceptIntent.action = TwilioSdk.ACTION_CALL
                acceptIntent.putExtra(
                    TwilioSdk.EXTRA_TYPE,
                    TwilioSdk.TYPE_ACCEPT
                )
                acceptIntent.putExtra(
                    TwilioSdk.EXTRA_CALL_OPTIONS,
                    Gson().toJson(callOptions)
                )
                val acceptPendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    acceptIntent,
                    getNotificationFlag()
                )

                val declineIntent = Intent(context, VideoCallReceiver::class.java)
                declineIntent.action = TwilioSdk.ACTION_CALL
                declineIntent.putExtra(
                    TwilioSdk.EXTRA_CALL_OPTIONS,
                    Gson().toJson(callOptions)
                )
                declineIntent.putExtra(
                    TwilioSdk.EXTRA_TYPE,
                    TwilioSdk.TYPE_REJECT
                )

                val declinePendingIntent = PendingIntent.getBroadcast(
                    context,
                    2,
                    declineIntent,
                    getNotificationFlag()
                )
                val notificationBuilder: NotificationCompat.Builder =
                    NotificationCompat.Builder(
                        context,
                        INCOMING_NOTIFICATION_CHANNEL_ID
                    ).setSmallIcon(R.drawable.twilio_ic_videocam_black_24dp)
                        .setContentTitle("Incoming call")
                        .setContentText("Maya Expert Calling")
                        .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setOngoing(true)
                        .setAutoCancel(true)
                        .setSound(Uri.parse("android.resource://" + context.packageName + "/" + R.raw.twilio_incoming_ringtone))
                        .addAction(
                            R.drawable.twilio_ic_baseline_call_24,
                            "Accept",
                            acceptPendingIntent
                        )
                        .addAction(
                            R.drawable.twilio_ic_baseline_call_end_24,
                            "Decline",
                            declinePendingIntent
                        )
                        .setFullScreenIntent(fullScreenPendingIntent, true)

                val incomingCallNotification = notificationBuilder.build()
                incomingCallNotification.flags = Notification.FLAG_INSISTENT
                return incomingCallNotification
            } else {
                return buildOngoingCallNotification(context, false)
            }
        } else {
            return buildOngoingCallNotification(context, false)
        }
    }

    fun getNotificationFlag() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT

    fun isAppForeground(context: Context) = !isAppBackground(context)
    private fun isAppBackground(context: Context): Boolean {
        var isInBackground = true
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        try {
            val runningProcesses = am.runningAppProcesses
            if (runningProcesses != null) {
                for (processInfo: ActivityManager.RunningAppProcessInfo in runningProcesses) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        for (activeProcess: String in processInfo.pkgList) {
                            if ((activeProcess == context.packageName)) {
                                isInBackground = false
                            }
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        return isInBackground
    }
}
