package com.project.fisionettest.ui.appointment

import android.os.Bundle
import com.project.fisionettest.R
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
import com.project.fisionettest.utils.AppPreferences
import androidx.navigation.fragment.findNavController
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AppointmentFragment : Fragment() {
    private var _binding: FragmentAppointmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var appointmentAdapter: AppointmentAdapter
    private lateinit var prefs: AppPreferences

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
    private var selectedStatusFilter: String = "Semua"
    private var patientList: List<com.project.fisionettest.data.model.Patient> = emptyList()
    private var selectedPatient: com.project.fisionettest.data.model.Patient? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPreferences(requireContext())

        setupRecyclerView()
        setupFilter()
        setupStatusFilter()
        loadAppointments()
        loadPatients()

        binding.fabAddAppointment.setOnClickListener {
            showAddAppointmentDialog()
        }
    }

    private fun setupStatusFilter() {
        binding.chipGroupStatus?.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedStatusFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_upcoming -> "Akan Datang"
                R.id.chip_completed -> "Selesai"
                R.id.chip_no_show -> "Tidak Hadir"
                else -> "Semua"
            }
            filterAppointments()
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
        var list = if (filteredDate == null) {
            allAppointments
        } else {
            allAppointments.filter { it.date == filteredDate }
        }

        list = when (selectedStatusFilter) {
            "Akan Datang" -> list.filter { it.status == "Terjadwal" }
            "Selesai" -> list.filter { it.status == "Selesai" }
            "Tidak Hadir" -> list.filter { it.status == "Dibatalkan" }
            else -> list
        }

        appointmentAdapter.submitList(list)

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
        appointmentAdapter.onServeClick = { appointment ->
            val pId = appointment.patient_id ?: 0
            if (pId > 0) {
                val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
                prefs.activeAppointmentId = appointment.id ?: -1
                val bundle = Bundle().apply {
                    putInt("patientId", pId)
                }
                findNavController().navigate(R.id.patientDetailFragment, bundle)
            } else {
                android.widget.Toast.makeText(requireContext(), "ID Pasien tidak valid", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAppointments() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            binding.rvAppointments.visibility = View.GONE
            try {
                SupabaseClient.autoMarkPastAppointmentsAsMissed()
                // Fetch all appointments for the therapist to see
                allAppointments = SupabaseClient.client.from("appointments").select(columns = Columns.raw("*, patients(*), profiles(*)")) {
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    order("time", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }.decodeList<Appointment>()
                
                filterAppointments()
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvAppointments.visibility = View.GONE
            } finally {
                binding.pbLoading.visibility = View.GONE
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
        
        // Setup Patient AutoCompleteTextView for Search
        val patientNames = patientList.map { it.name }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, patientNames)
        dialogBinding.actPatient.setAdapter(adapter)
        
        dialogBinding.actPatient.setOnItemClickListener { parent, view, position, id ->
            val selectedName = parent.getItemAtPosition(position).toString()
            selectedPatient = patientList.find { it.name == selectedName }
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
            val typedName = dialogBinding.actPatient.text.toString()
            selectedPatient = patientList.find { it.name.equals(typedName, ignoreCase = true) }

            if (selectedPatient == null) {
                android.widget.Toast.makeText(requireContext(), "Silakan pilih pasien yang terdaftar", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDate == null || selectedTime == null) {
                android.widget.Toast.makeText(requireContext(), "Silakan isi tanggal dan waktu", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                    try {
                        // Auth user id no longer needed for appointment creation
                        val dbStatus = "Terjadwal"
                        val newAppointment = kotlinx.serialization.json.buildJsonObject {
                            put("patient_id", selectedPatient?.id)
                            put("date", selectedDate!!)
                            put("time", selectedTime!!)
                            put("status", "Terjadwal")
                            put("notes", if (notes.isBlank()) null else notes)
                            // Auto-assign therapist via profile_id
                            val user = SupabaseClient.client.auth.currentUserOrNull()
                            put("profile_id", user?.id)
                        }
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
        
        // Setup Patient AutoCompleteTextView for Search
        val patientNames = patientList.map { it.name }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, patientNames)
        dialogBinding.actPatient.setAdapter(adapter)
        
        // Determine selected patient index
        val patientName = appointment.patients?.name ?: ""
        val currentPatientIndex = patientList.indexOfFirst { it.name == patientName }
        if (currentPatientIndex != -1) {
            dialogBinding.actPatient.setText(patientName, false)
            selectedPatient = patientList[currentPatientIndex]
        }
        
        dialogBinding.actPatient.setOnItemClickListener { parent, view, position, id ->
            val selectedName = parent.getItemAtPosition(position).toString()
            selectedPatient = patientList.find { it.name == selectedName }
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
            val typedName = dialogBinding.actPatient.text.toString()
            selectedPatient = patientList.find { it.name.equals(typedName, ignoreCase = true) }

            if (selectedPatient == null) {
                android.widget.Toast.makeText(requireContext(), "Silakan pilih pasien yang terdaftar", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDate == null || selectedTime == null) {
                android.widget.Toast.makeText(requireContext(), "Silakan isi tanggal dan waktu", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                    try {
                        val updatedAppointment = appointment.copy(
                            patient_id = selectedPatient?.id,
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
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmDelete(appointment: Appointment) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Appointment")
            .setMessage("Apakah Anda yakin ingin menghapus appointment untuk ${appointment.patients?.name ?: "Pasien"}?")
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
