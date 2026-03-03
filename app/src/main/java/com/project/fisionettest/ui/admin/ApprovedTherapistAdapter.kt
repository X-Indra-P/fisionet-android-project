package com.project.fisionettest.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.Profile
import com.project.fisionettest.databinding.ItemApprovedTherapistBinding

class ApprovedTherapistAdapter(
    private var therapists: List<Profile>
) : RecyclerView.Adapter<ApprovedTherapistAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemApprovedTherapistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: Profile) {
            binding.tvName.text = profile.displayName ?: "Tanpa Nama"
            binding.tvClinic.text = profile.clinic ?: "Belum ada cabang"
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
