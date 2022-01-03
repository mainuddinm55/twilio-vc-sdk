package info.learncoding.twiliovideocall.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import info.learncoding.twiliovideocall.data.model.CallOptions
import info.learncoding.twiliovideocall.di.RoomEntryPoint
import info.learncoding.twiliovideocall.ui.room.RoomManager
import info.learncoding.twiliovideocall.ui.room.RoomManagerProvider
import info.learncoding.twiliovideocall.TwilioSdk
import info.learncoding.twiliovideocall.ui.call.OnGoingCallActivity
import kotlinx.coroutines.*
import javax.inject.Inject

private const val TAG = "VideoCallReceiver"

@AndroidEntryPoint
class VideoCallReceiver : BroadcastReceiver() {
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)


    @Inject
    lateinit var provider: RoomManagerProvider
    private var roomManager: RoomManager? = null

    init {
        job.invokeOnCompletion {
            scope.cancel()
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: ${intent?.getStringExtra(TwilioSdk.EXTRA_TYPE)}")
        provider.roomComponent?.let { roomComponent ->
            EntryPoints.get(
                roomComponent, RoomEntryPoint::class.java
            ).getRoomManager().also { roomManager = it }
        }
        if (intent?.action == TwilioSdk.ACTION_CALL) {
            when (intent.getStringExtra(TwilioSdk.EXTRA_TYPE)) {
                TwilioSdk.TYPE_ACCEPT -> {
                    val callOptionsJson = intent.getStringExtra(
                        TwilioSdk.EXTRA_CALL_OPTIONS
                    )
                    val callOptions = Gson().fromJson(callOptionsJson, CallOptions::class.java)
                    Log.d(TAG, "onReceive: $callOptionsJson")
                    Log.d(TAG, "onReceive: $callOptions")
                    scope.launch {
                        roomManager?.connect(callOptions = callOptions ?: return@launch)
                    }
                    Intent(context, OnGoingCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        context?.startActivity(this)
                    }
                }
                TwilioSdk.TYPE_REJECT -> {
                    roomManager?.disconnect(isReject = true)
                }
                TwilioSdk.TYPE_END -> {
                    roomManager?.disconnect()
                }
                TwilioSdk.TYPE_MISSED_CALL -> {
                    roomManager?.disconnect(isMissedCall = true)
                }
                TwilioSdk.TYPE_RINGING -> {
                    roomManager?.setRinging()
                }
            }
        }
    }
}