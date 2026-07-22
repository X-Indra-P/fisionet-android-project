package com.project.fisionettest.ui.appointment

import android.graphics.Color
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
        holder.bind(getItem(position), onServeClick)
        holder.itemView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                onItemClick?.invoke(getItem(currentPosition))
            }
        }
    }

    var onItemClick: ((Appointment) -> Unit)? = null
    var onServeClick: ((Appointment) -> Unit)? = null

    class AppointmentViewHolder(
        private val binding: ItemAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment, onServeClick: ((Appointment) -> Unit)?) {
            val name = appointment.patients?.name ?: "N/A"
            binding.tvPatientName.text = name

            // Avatar initial — huruf pertama nama pasien
            binding.tvAvatarInitial.text = name.firstOrNull()?.uppercase() ?: "?"

            // Format date
            binding.tvDate.text = formatDate(appointment.date)

            // Format time (trim seconds if present: "08:00:00" -> "08:00")
            binding.tvTime.text = appointment.time.take(5)

            // Notes
            if (!appointment.notes.isNullOrBlank()) {
                binding.tvNotes.text = appointment.notes
                binding.tvNotes.visibility = android.view.View.VISIBLE
            } else {
                binding.tvNotes.visibility = android.view.View.GONE
            }

            // Map DB Status -> UI display label + color
            val uiStatus = when (appointment.status) {
                "Terjadwal" -> "Menunggu"
                "Selesai"   -> "Hadir"
                "Dibatalkan" -> "Tidak Hadir"
                else        -> appointment.status ?: "-"
            }
            binding.tvStatus.text = uiStatus

            // Status chip color + accent bar color
            val statusColor = when (uiStatus) {
                "Menunggu"    -> "#F59E0B" // Amber
                "Hadir"       -> "#10B981" // Green
                "Tidak Hadir" -> "#EF4444" // Red
                else          -> "#6B7280" // Gray
            }
            binding.tvStatus.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor(statusColor))

            // Therapist details
            val therapistText = appointment.profiles?.displayName ?: "-"
            binding.tvAppointmentDetails?.text = "Pembuat: $therapistText"

            // Left accent bar color matches chip
            binding.viewAccent.setBackgroundColor(Color.parseColor(statusColor))

            // Show Serve button only if status is "Terjadwal"
            if (appointment.status == "Terjadwal") {
                binding.btnServe.visibility = android.view.View.VISIBLE
                binding.btnServe.setOnClickListener {
                    onServeClick?.invoke(appointment)
                }
            } else {
                binding.btnServe.visibility = android.view.View.GONE
            }
        }

        private fun formatDate(dateStr: String?): String {
            if (dateStr.isNullOrBlank()) return "—"
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val display = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
                val date = sdf.parse(dateStr)
                if (date != null) display.format(date) else dateStr
            } catch (e: Exception) {
                dateStr
            }
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
