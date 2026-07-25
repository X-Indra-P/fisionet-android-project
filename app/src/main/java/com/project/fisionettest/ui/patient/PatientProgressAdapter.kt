package com.project.fisionettest.ui.patient

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.PatientProgress
import com.project.fisionettest.databinding.ItemPatientProgressBinding

class PatientProgressAdapter : ListAdapter<PatientProgress, PatientProgressAdapter.ProgressViewHolder>(ProgressDiffCallback()) {

    private var toolsMap: Map<Int, String> = emptyMap()

    fun updateToolsMap(map: Map<Int, String>) {
        toolsMap = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val binding = ItemPatientProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(getItem(position), toolsMap)
    }

    class ProgressViewHolder(private val binding: ItemPatientProgressBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(progress: PatientProgress, toolsMap: Map<Int, String>) {
            binding.tvProgressDate.text = "Tanggal: ${progress.date}"
            
            val selectedPackage = progress.cabang_package?.packages
            if (selectedPackage != null) {
                binding.tvProgressTindakan.visibility = android.view.View.VISIBLE
                binding.tvProgressTindakan.text = "Paket Terapi: ${selectedPackage.name}"
                
                val toolIds = progress.cabang_package.id_tools
                val tools = toolIds?.mapNotNull { toolsMap[it] }?.joinToString(", ")
                binding.tvProgressTools.visibility = android.view.View.VISIBLE
                binding.tvProgressTools.text = "Alat Terapi: ${tools ?: "-"}"
            } else {
                binding.tvProgressTindakan.visibility = android.view.View.GONE
                binding.tvProgressTools.visibility = android.view.View.GONE
            }

            val currentStatus = progress.status ?: "Proses"
            binding.tvProgressStatus.text = currentStatus
            if (currentStatus.equals("Selesai", ignoreCase = true)) {
                binding.tvProgressStatus.setBackgroundResource(com.project.fisionettest.R.drawable.bg_badge_active)
            } else {
                binding.tvProgressStatus.setBackgroundResource(com.project.fisionettest.R.drawable.bg_badge_inactive)
            }
            
            binding.tvProgressNote.text = "Catatan perkembangan: ${progress.progress_note.ifBlank { "Belum ada catatan" }}"
            binding.tvProgressCabang.text = "Cabang: ${com.project.fisionettest.utils.ClinicMapper.toName(progress.id_cabang, progress.cabang)}"
            binding.tvProgressTherapist.text = "Terapis: ${progress.profiles?.displayName ?: "-"}"
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
