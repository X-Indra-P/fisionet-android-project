package com.project.fisionettest.ui.patient

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.MedicalRecord
import com.project.fisionettest.databinding.ItemMedicalRecordBinding

class MedicalRecordAdapter(
    private val onItemClick: (MedicalRecord) -> Unit
) : ListAdapter<MedicalRecord, MedicalRecordAdapter.MedicalRecordViewHolder>(MedicalRecordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicalRecordViewHolder {
        val binding = ItemMedicalRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MedicalRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicalRecordViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class MedicalRecordViewHolder(
        private val binding: ItemMedicalRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: MedicalRecord, onItemClick: (MedicalRecord) -> Unit) {
            binding.tvDate.text = "Tanggal: ${record.date}"
            binding.tvDiagnosis.text = "Diagnosis: ${record.diagnosis}"
            binding.tvVitalSign.text = "Vital Sign: ${record.vital_sign}"

            binding.root.setOnClickListener {
                onItemClick(record)
            }
        }
    }

    class MedicalRecordDiffCallback : DiffUtil.ItemCallback<MedicalRecord>() {
        override fun areItemsTheSame(oldItem: MedicalRecord, newItem: MedicalRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MedicalRecord, newItem: MedicalRecord): Boolean {
            return oldItem == newItem
        }
    }
}
