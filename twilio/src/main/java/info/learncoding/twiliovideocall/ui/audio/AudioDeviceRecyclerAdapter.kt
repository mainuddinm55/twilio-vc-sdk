package info.learncoding.twiliovideocall.ui.audio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.twilio.audioswitch.AudioDevice
import info.learncoding.twiliovideocall.R
import info.learncoding.twiliovideocall.databinding.TwilioRowItemAudioDeviceBinding

class AudioDeviceRecyclerAdapter(
    private val audioDevices: List<AudioDevice>,
    private val selectedAudioDevice: String?,
    private val listener: (selectDevice: AudioDevice?) -> Unit
) : RecyclerView.Adapter<AudioDeviceRecyclerAdapter.AudioDeviceRecyclerViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DEVICE = 1
        private const val VIEW_TYPE_CANCEL = 2
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AudioDeviceRecyclerViewHolder {
        val binding = TwilioRowItemAudioDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val viewHolder = AudioDeviceRecyclerViewHolder(binding)
        try {
            viewHolder.itemView.setOnClickListener { v: View? ->
                try {
                    if (viewType == VIEW_TYPE_DEVICE) {
                        listener.invoke(audioDevices[viewHolder.adapterPosition])
                    } else if (viewType == VIEW_TYPE_CANCEL) {
                        listener.invoke(null)
                    }
                } catch (exception: java.lang.Exception) {
                    exception.printStackTrace()
                }

            }
        } catch (ignored: java.lang.Exception) {
        }
        return viewHolder
    }

    override fun onBindViewHolder(
        holder: AudioDeviceRecyclerViewHolder,
        position: Int
    ) {
        val item = try {
            audioDevices[position]
        } catch (e: Exception) {
            null
        } catch (e: java.lang.Exception) {
            null
        }
        val isRtl = holder.binding.nameTextView.layoutDirection ==
                View.LAYOUT_DIRECTION_RTL
        when (holder.itemViewType) {
            VIEW_TYPE_DEVICE -> {
                item?.let {
                    val audioDeviceMenuIcon = when (it) {
                        is AudioDevice.BluetoothHeadset -> R.drawable.twilio_ic_bluetooth_white_24dp
                        is AudioDevice.WiredHeadset -> R.drawable.twilio_ic_headset_mic_white_24dp
                        is AudioDevice.Speakerphone -> R.drawable.twilio_ic_volume_up_white_24dp
                        else -> R.drawable.twilio_ic_phonelink_ring_white_24dp
                    }
                    val selectedIcon = if (it.name == selectedAudioDevice) {
                        R.drawable.twilio_ic_baseline_check_24
                    } else {
                        0
                    }
                    holder.binding.nameTextView.setCompoundDrawablesWithIntrinsicBounds(
                        if (isRtl) selectedIcon else audioDeviceMenuIcon, 0,
                        if (isRtl) audioDeviceMenuIcon else selectedIcon, 0
                    )
                    holder.binding.nameTextView.text = it.name
                }
            }
            VIEW_TYPE_CANCEL -> {
                holder.binding.nameTextView.text = "Cancel"
                holder.binding.nameTextView.setCompoundDrawablesWithIntrinsicBounds(
                    if (isRtl) 0 else R.drawable.twilio_ic_baseline_close_24, 0,
                    if (isRtl) R.drawable.twilio_ic_baseline_close_24 else 0, 0
                )
            }
        }

        holder.setIsRecyclable(false)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) {
            VIEW_TYPE_CANCEL
        } else {
            VIEW_TYPE_DEVICE
        }
    }

    override fun getItemCount(): Int {
        return audioDevices.size + 1
    }

    inner class AudioDeviceRecyclerViewHolder(val binding: TwilioRowItemAudioDeviceBinding) :
        RecyclerView.ViewHolder(binding.nameTextView) {}
}