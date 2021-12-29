package info.learncoding.twiliosdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import info.learncoding.twiliovideocall.TwilioSdk

private const val TAG = "VideoCallListenerRecevi"

class VideoCallListenerReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, intent: Intent?) {
        if (intent?.action == TwilioSdk.ACTION_CALL_DATA) {
            Log.d(TAG, "onReceive: key-> ${intent.getStringExtra(TwilioSdk.EXTRA_CALL_DATA_KEY)}")
            intent.extras?.keySet()?.forEach {
                Log.d(TAG, "onReceive: key-> $it value-> ${intent.extras?.get(it)}")
            }
        }
    }
}