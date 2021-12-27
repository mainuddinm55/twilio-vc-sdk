package info.learncoding.twiliosdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

private const val TAG = "VideoCallListenerRecevi"

class VideoCallListenerReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d(TAG, "onReceive: ${Date()}")
    }
}