package com.project.fisionettest.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.Profile
import com.project.fisionettest.databinding.ItemApprovedTherapistBinding

class ApprovedTherapistAdapter(
    private var therapists: List<Profile>,
    private val onTransferBranch: (Profile) -> Unit = {}
) : RecyclerView.Adapter<ApprovedTherapistAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemApprovedTherapistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: Profile) {
            val name = profile.displayName ?: "Tanpa Nama"
            binding.tvName.text = name
            binding.tvAvatarInitial.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "T"
            binding.chipClinic.text = profile.clinic ?: "Belum ada cabang"

            binding.btnTransferBranch.setOnClickListener {
                onTransferBranch(profile)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApprovedTherapistBinding.inflate(
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
