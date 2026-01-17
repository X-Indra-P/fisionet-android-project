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
    private var patientList: List<com.project.fisionettest.data.model.Patient> = emptyList()
    private var selectedPatient: com.project.fisionettest.data.model.Patient? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilter()
        loadAppointments()
        loadPatients() // Fetch patients for dropdown

        binding.fabAddAppointment.setOnClickListener {
            showAddAppointmentDialog()
        }
    }

    private fun loadPatients() {
        lifecycleScope.launch {
            try {
                patientList = SupabaseClient.client.from("patients").select().decodeList()
            } catch (e: Exception) {
                // Fail silently or log
                e.printStackTrace()
            }
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
        
        // Setup Patient Spinner
        val patientNames = patientList.map { it.name }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, patientNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerPatient.adapter = adapter
        
        dialogBinding.spinnerPatient.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPatient = patientList.getOrNull(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedPatient = null
            }
        }

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
            val notes = dialogBinding.etNotes.text.toString()

            if (selectedPatient != null && selectedDate != null && selectedTime != null) {
                lifecycleScope.launch {
                    try {
                        val authUser = SupabaseClient.client.auth.currentSessionOrNull()?.user
                        val therapistId = authUser?.id ?: return@launch

                        val dbStatus = "Terjadwal"

                        val newAppointment = Appointment(
                            patient_id = selectedPatient?.id,
                            patient_name = selectedPatient!!.name,
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
                android.widget.Toast.makeText(requireContext(), "Mohon pilih pasien dan lengkapi data", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showStatusDialog(appointment: Appointment) {
        val options = arrayOf("Edit", "Menunggu", "Hadir", "Tidak Hadir", "Hapus")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Opsi Appointment")
            .setItems(options) { _, which ->
                when (val selectedUiStatus = options[which]) {
                    "Edit" -> showEditAppointmentDialog(appointment)
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
    
    private fun showEditAppointmentDialog(appointment: Appointment) {
        val dialogBinding = com.project.fisionettest.databinding.DialogAddAppointmentBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Hide Add Title, maybe change to "Edit Appointment" if TextView id was accessible/dynamic, 
        // but current layout has hardcoded text. We can ignore or code defensively if we had the ID.
        // Assuming the ID isn't exposed or simply relying on context.

        var selectedDate: String? = appointment.date
        var selectedTime: String? = appointment.time
        
        // Setup Patient Spinner
        val patientNames = patientList.map { it.name }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, patientNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerPatient.adapter = adapter
        
        // Determine selected patient index
        val currentPatientIndex = patientList.indexOfFirst { it.name == appointment.patient_name }
        if (currentPatientIndex != -1) {
            dialogBinding.spinnerPatient.setSelection(currentPatientIndex)
            selectedPatient = patientList[currentPatientIndex]
        }
        
        dialogBinding.spinnerPatient.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPatient = patientList.getOrNull(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Keep previous selection if possible or null
            }
        }

        // Fill existing data
        dialogBinding.etDate.setText(appointment.date)
        dialogBinding.etTime.setText(appointment.time.substring(0, 5)) // HH:mm
        dialogBinding.etNotes.setText(appointment.notes ?: "")

        dialogBinding.etDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            // Parse existing date if needed, or just use current
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
            val notes = dialogBinding.etNotes.text.toString()

            if (selectedPatient != null && selectedDate != null && selectedTime != null) {
                lifecycleScope.launch {
                    try {
                        val updatedAppointment = appointment.copy(
                            patient_id = selectedPatient?.id,
                            patient_name = selectedPatient!!.name,
                            date = selectedDate!!,
                            time = selectedTime!!,
                            notes = if (notes.isBlank()) null else notes
                        )

                        SupabaseClient.client.from("appointments").update(updatedAppointment) {
                            filter { eq("id", appointment.id!!) }
                        }
                        
                        loadAppointments()
                        dialog.dismiss()
                        android.widget.Toast.makeText(requireContext(), "Appointment diperbarui", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(requireContext(), "Gagal Update: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                android.widget.Toast.makeText(requireContext(), "Semua data harus diisi", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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
