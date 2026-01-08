package com.project.fisionettest.ui.appointment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Appointment
import com.project.fisionettest.databinding.FragmentAppointmentBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.launch

class AppointmentFragment : Fragment() {
    private var _binding: FragmentAppointmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var appointmentAdapter: AppointmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var allAppointments: List<Appointment> = emptyList()
    private var filteredDate: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilter()
        loadAppointments()

        binding.fabAddAppointment.setOnClickListener {
            showAddAppointmentDialog()
        }
    }

    private fun setupFilter() {
        binding.etFilterDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
                filteredDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                binding.etFilterDate.setText(filteredDate)
                binding.btnClearFilter.visibility = View.VISIBLE
                filterAppointments()
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnClearFilter.setOnClickListener {
            filteredDate = null
            binding.etFilterDate.setText("")
            binding.btnClearFilter.visibility = View.GONE
            filterAppointments()
        }
    }

    private fun filterAppointments() {
        val list = if (filteredDate == null) {
            allAppointments
        } else {
            allAppointments.filter { it.date == filteredDate }
        }
        appointmentAdapter.submitList(list)
        
        binding.tvEmpty.text = "Tidak ada appointment yang ditemukan"
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAppointments.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter()
        binding.rvAppointments.apply {
            adapter = appointmentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        appointmentAdapter.onItemClick = { appointment ->
            showStatusDialog(appointment)
        }
    }

    private fun loadAppointments() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Order by date descending (newest first)
                allAppointments = SupabaseClient.client.from("appointments").select {
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    order("time", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }.decodeList<Appointment>()
                
                filterAppointments()
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvEmpty.text = "Error memuat data: ${e.message}"
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvAppointments.visibility = View.GONE
            }
        }
    }


    private fun showAddAppointmentDialog() {
        val dialogBinding = com.project.fisionettest.databinding.DialogAddAppointmentBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        var selectedDate: String? = null
        var selectedTime: String? = null

        dialogBinding.etDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                dialogBinding.etDate.setText(selectedDate)
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        dialogBinding.etTime.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedTime = String.format("%02d:%02d:00", hour, minute)
                dialogBinding.etTime.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE), true).show()
        }

        dialogBinding.btnSave.setOnClickListener {
            val patientName = dialogBinding.etPatient.text.toString()
            val notes = dialogBinding.etNotes.text.toString()

            if (patientName.isNotBlank() && selectedDate != null && selectedTime != null) {
                lifecycleScope.launch {
                    try {
                        val authUser = SupabaseClient.client.auth.currentSessionOrNull()?.user
                        val therapistId = authUser?.id ?: return@launch

                        val dbStatus = "Terjadwal"

                        val newAppointment = Appointment(
                            patient_id = null,
                            patient_name = patientName,
                            therapist_id = therapistId,
                            date = selectedDate!!,
                            time = selectedTime!!,
                            status = dbStatus,
                            notes = if (notes.isBlank()) null else notes
                        )
                        SupabaseClient.client.from("appointments").insert(newAppointment)
                        loadAppointments()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Gagal Menyimpan")
                            .setMessage("Error: ${e.message}")
                            .setPositiveButton("OK", null)
                            .show()
                        e.printStackTrace()
                    }
                }
            } else {
                android.widget.Toast.makeText(requireContext(), "Mohon lengkapi data", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showStatusDialog(appointment: Appointment) {
        val options = arrayOf("Menunggu", "Hadir", "Tidak Hadir", "Hapus")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Opsi Appointment")
            .setItems(options) { _, which ->
                when (val selectedUiStatus = options[which]) {
                    "Hapus" -> confirmDelete(appointment)
                    else -> {
                        val dbStatus = when (selectedUiStatus) {
                            "Menunggu" -> "Terjadwal"
                            "Hadir" -> "Selesai"
                            "Tidak Hadir" -> "Dibatalkan"
                            else -> "Terjadwal"
                        }
                        updateAppointmentStatus(appointment, dbStatus)
                    }
                }
            }
            .show()
    }

    private fun confirmDelete(appointment: Appointment) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Appointment")
            .setMessage("Apakah Anda yakin ingin menghapus appointment untuk ${appointment.patient_name}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteAppointment(appointment)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteAppointment(appointment: Appointment) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.from("appointments").delete {
                    filter {
                        eq("id", appointment.id!!)
                    }
                }
                loadAppointments()
                android.widget.Toast.makeText(context, "Appointment dihapus", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Gagal Menghapus")
                    .setMessage(e.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun updateAppointmentStatus(appointment: Appointment, status: String) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.from("appointments").update({
                    set("status", status)
                }) {
                    filter {
                        eq("id", appointment.id!!)
                    }
                }
                loadAppointments()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Gagal Update Status")
                    .setMessage(e.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}
