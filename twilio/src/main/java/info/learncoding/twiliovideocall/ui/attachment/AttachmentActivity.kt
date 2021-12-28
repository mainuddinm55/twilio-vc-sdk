package info.learncoding.twiliovideocall.ui.attachment

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import info.learncoding.twiliovideocall.R
import info.learncoding.twiliovideocall.databinding.TwilioActivityAttachmentBinding

class AttachmentActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_ATTACHMENTS = "attachments"
        fun buildIntent(context: Context, attachments: ArrayList<String>): Intent {
            return Intent(context, AttachmentActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_ATTACHMENTS, attachments)
            }
        }

        fun getAttachments(intent: Intent): List<String> {
            return intent.getStringArrayListExtra(EXTRA_ATTACHMENTS) ?: emptyList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = TwilioActivityAttachmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.closeImageView.setOnClickListener {
            finish()
        }
        val attachments = getAttachments(intent)
        if (attachments.isEmpty()) {
            finish()
            return
        }
        val attachmentRecyclerAdapter = AttachmentRecyclerAdapter(attachments)
        binding.viewPager.adapter = attachmentRecyclerAdapter
    }
}