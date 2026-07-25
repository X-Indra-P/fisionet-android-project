package com.project.fisionettest.ui.patient

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.R
import com.project.fisionettest.data.model.Diagnosis
import com.project.fisionettest.databinding.ItemDiagnosisBinding

class DiagnosisAdapter(
    private val onItemClick: (Diagnosis) -> Unit
) : ListAdapter<Diagnosis, DiagnosisAdapter.DiagnosisViewHolder>(DiagnosisDiffCallback()) {

    private var packageToolsMap: Map<String, String> = emptyMap()
    private var toolsToPackageMap: Map<String, String> = emptyMap()
    private var diagnosisStatusMap: Map<Int?, String> = emptyMap()

    fun updateExtraData(
        toolsMap: Map<String, String>,
        statusMap: Map<Int?, String>,
        toolsToPkgMap: Map<String, String> = emptyMap()
    ) {
        packageToolsMap = toolsMap
        diagnosisStatusMap = statusMap
        toolsToPackageMap = toolsToPkgMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiagnosisViewHolder {
        val binding = ItemDiagnosisBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiagnosisViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiagnosisViewHolder, position: Int) {
        holder.bind(getItem(position), packageToolsMap, toolsToPackageMap, diagnosisStatusMap, onItemClick)
    }

    class DiagnosisViewHolder(
        private val binding: ItemDiagnosisBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            record: Diagnosis,
            packageToolsMap: Map<String, String>,
            toolsToPackageMap: Map<String, String>,
            diagnosisStatusMap: Map<Int?, String>,
            onItemClick: (Diagnosis) -> Unit
        ) {
            binding.tvDate.text = "Tanggal: ${record.date}"
            binding.tvDiagnosis.text = "Diagnosis: ${record.diagnosa}"
            binding.tvVitalSign.text = "Vital Sign: ${record.vital_sign}"
            binding.tvTherapistName.text = "Terapis: ${record.profiles?.displayName ?: "-"}"
            binding.tvCabang.text = "Cabang: ${com.project.fisionettest.utils.ClinicMapper.toName(record.id_cabang, record.cabang)}"
            
            val pkgName = record.cabang_package?.packages?.name
            val toolsText = if (pkgName != null) packageToolsMap[pkgName] ?: "-" else "-"
            binding.tvPlanning.text = "Planning: $toolsText"
            binding.tvTools.visibility = android.view.View.GONE

            val status = record.status
            if (status == "Selesai") {
                binding.tvStatus.text = "Selesai"
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_active)
            } else {
                binding.tvStatus.text = "Proses"
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_inactive)
            }

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
