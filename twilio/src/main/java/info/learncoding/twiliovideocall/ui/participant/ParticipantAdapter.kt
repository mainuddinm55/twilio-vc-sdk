package info.learncoding.twiliovideocall.ui.participant

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

internal class ParticipantAdapter : ListAdapter<ParticipantViewState, ParticipantViewHolder>(
    ParticipantDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder =
        ParticipantViewHolder(ParticipantThumbView(parent.context))

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) =
        holder.bind(getItem(position))

    class ParticipantDiffCallback : DiffUtil.ItemCallback<ParticipantViewState>() {
        override fun areItemsTheSame(
            oldItem: ParticipantViewState,
            newItem: ParticipantViewState
        ): Boolean =
            oldItem.sid == newItem.sid

        override fun areContentsTheSame(
            oldItem: ParticipantViewState,
            newItem: ParticipantViewState
        ): Boolean = oldItem == newItem

        override fun getChangePayload(
            oldItem: ParticipantViewState,
            newItem: ParticipantViewState
        ): Any {
            return newItem
        }
    }
}
