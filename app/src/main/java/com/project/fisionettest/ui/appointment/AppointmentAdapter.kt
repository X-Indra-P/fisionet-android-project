package com.project.fisionettest.ui.appointment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.Appointment
import com.project.fisionettest.databinding.ItemAppointmentBinding

class AppointmentAdapter : ListAdapter<Appointment, AppointmentAdapter.AppointmentViewHolder>(AppointmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(getItem(position))
        }
    }

    var onItemClick: ((Appointment) -> Unit)? = null

    class AppointmentViewHolder(
        private val binding: ItemAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            binding.tvPatientName.text = "Pasien: ${appointment.patient_name ?: "N/A"}"
            binding.tvDate.text = "Tanggal: ${appointment.date}"
            binding.tvTime.text = "Waktu: ${appointment.time}"
            
            if (!appointment.notes.isNullOrBlank()) {
                binding.tvNotes.text = "Catatan: ${appointment.notes}"
                binding.tvNotes.visibility = android.view.View.VISIBLE
            } else {
                binding.tvNotes.visibility = android.view.View.GONE
            }

            // Map DB Status -> UI Status for display
            val uiStatus = when (appointment.status) {
                "Terjadwal" -> "Menunggu"
                "Selesai" -> "Hadir"
                "Dibatalkan" -> "Tidak Hadir"
                else -> appointment.status // Fallback
            }
            binding.tvStatus.text = "Status: $uiStatus"

            // Set Color
            val colorRes = when (uiStatus) {
                "Menunggu" -> com.project.fisionettest.R.color.status_waiting
                "Hadir" -> com.project.fisionettest.R.color.status_present
                "Tidak Hadir" -> com.project.fisionettest.R.color.status_absent
                else -> com.project.fisionettest.R.color.black
            }
            binding.tvStatus.setTextColor(itemView.context.getColor(colorRes))
        }
    }

    class AppointmentDiffCallback : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem == newItem
        }
    }
}
