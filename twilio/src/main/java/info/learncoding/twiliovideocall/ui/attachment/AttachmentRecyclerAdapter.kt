package info.learncoding.twiliovideocall.ui.attachment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import info.learncoding.twiliovideocall.R
import info.learncoding.twiliovideocall.databinding.TwilioRowItemFullScreenImageBinding

class AttachmentRecyclerAdapter(
    private val attachments: List<String>
) : RecyclerView.Adapter<AttachmentRecyclerAdapter.AttachmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
        return AttachmentViewHolder(
            TwilioRowItemFullScreenImageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        Glide.with(holder.itemView)
            .load(attachments[position])
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
            .apply(RequestOptions.placeholderOf(R.drawable.twilio_ic_baseline_image_24))
            .apply(RequestOptions.errorOf(R.drawable.twilio_ic_baseline_image_24))
            .into(holder.binding.attachmentImageView)
    }

    override fun getItemCount(): Int {
        return attachments.size
    }

    inner class AttachmentViewHolder(val binding: TwilioRowItemFullScreenImageBinding) :
        RecyclerView.ViewHolder(binding.attachmentImageView)
}