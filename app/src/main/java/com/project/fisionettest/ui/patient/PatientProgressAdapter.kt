package com.project.fisionettest.ui.patient

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.PatientProgress
import com.project.fisionettest.databinding.ItemPatientProgressBinding

class PatientProgressAdapter : ListAdapter<PatientProgress, PatientProgressAdapter.ProgressViewHolder>(ProgressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val binding = ItemPatientProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProgressViewHolder(private val binding: ItemPatientProgressBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(progress: PatientProgress) {
            binding.tvProgressDate.text = progress.date
            binding.tvProgressNote.text = progress.progress_note
        }
    }

    class ProgressDiffCallback : DiffUtil.ItemCallback<PatientProgress>() {
        override fun areItemsTheSame(oldItem: PatientProgress, newItem: PatientProgress): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PatientProgress, newItem: PatientProgress): Boolean {
            return oldItem == newItem
        }
    }
}
