package info.learncoding.twiliovideocall.ui.audio

import android.app.Dialog
import android.os.Bundle
import android.view.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.twilio.audioswitch.AudioDevice
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import info.learncoding.twiliovideocall.databinding.TwilioAudioDevicesBottomSheetBinding
import info.learncoding.twiliovideocall.di.RoomEntryPoint
import info.learncoding.twiliovideocall.ui.room.RoomManager
import info.learncoding.twiliovideocall.ui.room.RoomManagerProvider
import javax.inject.Inject

@AndroidEntryPoint
class AudioDevicesBottomSheetFragment : BottomSheetDialogFragment() {
    var deviceSelectedListener: ((device: AudioDevice?) -> Unit)? = null
    private lateinit var audioDevicesLayoutBinding: TwilioAudioDevicesBottomSheetBinding

    @Inject
    lateinit var provider: RoomManagerProvider

    private var roomManager: RoomManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        audioDevicesLayoutBinding = TwilioAudioDevicesBottomSheetBinding.inflate(
            inflater, container, false
        )
        return audioDevicesLayoutBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        context?.let {
            provider.roomComponent?.let { component ->
                EntryPoints.get(component, RoomEntryPoint::class.java)
            }?.getRoomManager().also {
                roomManager = it
            }
            val audioDeviceRecyclerAdapter = AudioDeviceRecyclerAdapter(
                roomManager?.viewState?.value?.availableAudioDevices ?: listOf(),
                roomManager?.viewState?.value?.selectedDevice?.name
            ) {
                dismiss()
                deviceSelectedListener?.invoke(it)
            }
            audioDevicesLayoutBinding.devicesRecyclerView.apply {
                setHasFixedSize(true)
                adapter = audioDeviceRecyclerAdapter
            }

        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window = dialog.window
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        return dialog
    }
}