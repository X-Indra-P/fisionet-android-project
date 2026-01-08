package com.project.fisionettest.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.databinding.ItemPatientBinding

class PatientAdapter(
    private val onItemClick: (Patient) -> Unit
) : ListAdapter<Patient, PatientAdapter.PatientViewHolder>(PatientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatientViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PatientViewHolder(
        private val binding: ItemPatientBinding,
        private val onItemClick: (Patient) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(patient: Patient) {
            binding.tvPatientName.text = patient.name
            binding.tvPatientAge.text = "Umur: ${patient.umur ?: 0} tahun"
            binding.tvPatientPhone.text = "Telepon: ${patient.phone ?: "-"}"
            binding.tvPatientOccupation.text = "Pekerjaan: ${patient.pekerjaan ?: "-"}"

            binding.root.setOnClickListener {
                onItemClick(patient)
            }
        }
    }

    class PatientDiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem == newItem
        }
    }
}
