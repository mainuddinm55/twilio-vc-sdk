package info.learncoding.twiliosdk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import info.learncoding.twiliovideocall.data.model.CallOptions
import info.learncoding.twiliovideocall.data.model.UserType
import info.learncoding.twiliovideocall.service.VideoCallService

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
                    caller,
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
                    receiver,
                    UserType.RECEIVER
                ),
                VideoCallListenerReceiver()
            )
        }
    }
}