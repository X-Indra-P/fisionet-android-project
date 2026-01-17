package com.project.fisionettest.ui.patient

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.Diagnosis
import com.project.fisionettest.databinding.ItemDiagnosisBinding

class DiagnosisAdapter(
    private val onItemClick: (Diagnosis) -> Unit
) : ListAdapter<Diagnosis, DiagnosisAdapter.DiagnosisViewHolder>(DiagnosisDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiagnosisViewHolder {
        val binding = ItemDiagnosisBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiagnosisViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiagnosisViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class DiagnosisViewHolder(
        private val binding: ItemDiagnosisBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: Diagnosis, onItemClick: (Diagnosis) -> Unit) {
            binding.tvDate.text = "Tanggal: ${record.date}"
            binding.tvDiagnosis.text = "Diagnosis: ${record.diagnosa}"
            binding.tvVitalSign.text = "Vital Sign: ${record.vital_sign}"

            binding.root.setOnClickListener {
                onItemClick(record)
            }
        }
    }

    class DiagnosisDiffCallback : DiffUtil.ItemCallback<Diagnosis>() {
        override fun areItemsTheSame(oldItem: Diagnosis, newItem: Diagnosis): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Diagnosis, newItem: Diagnosis): Boolean {
            return oldItem == newItem
        }
    }
}
