package com.project.fisionettest.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.Profile
import com.project.fisionettest.databinding.ItemPendingTherapistBinding

class PendingTherapistAdapter(
    private var therapists: List<Profile>,
    private val onApprove: (Profile) -> Unit,
    private val onReject: (Profile) -> Unit
) : RecyclerView.Adapter<PendingTherapistAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPendingTherapistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: Profile) {
            val name = profile.displayName ?: "Tanpa Nama"
            binding.tvName.text = name
            binding.tvAvatarInitial.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "T"
            binding.chipStatus.text = "Menunggu Verifikasi"

            binding.btnApprove.setOnClickListener {
                onApprove(profile)
            }

            binding.btnReject.setOnClickListener {
                onReject(profile)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingTherapistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(therapists[position])
    }

    override fun getItemCount(): Int = therapists.size

    fun updateList(newList: List<Profile>) {
        therapists = newList
        notifyDataSetChanged()
    }
}
