package com.project.fisionettest.ui.patient

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.data.model.PatientProgress
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.databinding.FragmentPatientDetailBinding
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Calendar

class PatientDetailFragment : Fragment() {
    private var _binding: FragmentPatientDetailBinding? = null
    private val binding get() = _binding!!
    private var patientId: Int = 0
    private lateinit var diagnosisAdapter: DiagnosisAdapter

    private var currentPatient: Patient? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        patientId = arguments?.getInt("patientId") ?: 0

        val prefs = AppPreferences(requireContext())
        if (prefs.userRole == 2) {
            binding.btnEditPatient.visibility = View.GONE
            binding.btnDeletePatient.visibility = View.GONE
        } else if (prefs.userRole == 1) {
            binding.btnAddMedicalRecord.visibility = View.GONE
        }

        setupRecyclerView()
        loadPatientData()

        binding.btnEditPatient.setOnClickListener {
            currentPatient?.id?.let { id ->
                val bundle = Bundle().apply {
                    putInt("patientId", id)
                }
                findNavController().navigate(R.id.action_patient_detail_to_edit, bundle)
            }
        }

        binding.btnDeletePatient.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.btnAddMedicalRecord.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("patientId", patientId)
            }
            findNavController().navigate(R.id.action_patient_detail_to_add_diagnosis, bundle)
        }

        binding.btnBack.setOnClickListener {
            navigateBackToPatientList()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToPatientList()
            }
        })

        binding.btnPatientHistory.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("patientId", patientId)
            }
            findNavController().navigate(R.id.action_patient_detail_to_transaction_history, bundle)
        }

        binding.btnScheduleAppointment.setOnClickListener {
            showAddAppointmentDialog()
        }

    }

    private fun navigateBackToPatientList() {
        val hasAdminDashboardInBackStack = try {
            findNavController().getBackStackEntry(R.id.adminDashboardFragment)
            true
        } catch (e: Exception) {
            false
        }

        val hasActivePatientsInBackStack = try {
            findNavController().getBackStackEntry(R.id.activePatientsFragment)
            true
        } catch (e: Exception) {
            false
        }

        val hasHomeInBackStack = try {
            findNavController().getBackStackEntry(R.id.homeFragment)
            true
        } catch (e: Exception) {
            false
        }

        if (hasAdminDashboardInBackStack) {
            findNavController().popBackStack(R.id.adminDashboardFragment, false)
        } else if (hasActivePatientsInBackStack) {
            findNavController().popBackStack(R.id.activePatientsFragment, false)
        } else if (hasHomeInBackStack) {
            findNavController().popBackStack(R.id.homeFragment, false)
        } else {
            findNavController().navigate(
                R.id.homeFragment,
                null,
                androidx.navigation.navOptions {
                    popUpTo(R.id.dashboardFragment) {
                        inclusive = false
                    }
                }
            )
        }
    }

    private fun setupRecyclerView() {
        diagnosisAdapter = DiagnosisAdapter { record ->
            val json = Json.encodeToString(com.project.fisionettest.data.model.Diagnosis.serializer(), record)
            val bundle = Bundle().apply {
                putString("diagnosis", json)
            }
            findNavController().navigate(R.id.action_patient_detail_to_diagnosis_detail, bundle)
        }
        binding.rvMedicalRecords.apply {
            adapter = diagnosisAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadPatientData() {
        lifecycleScope.launch {
            _binding?.pbLoading?.visibility = View.VISIBLE
            _binding?.tvEmptyRecords?.visibility = View.GONE
            try {
                // Load patient
                val patient = SupabaseClient.client.from("patients").select(
                    columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, profiles(*)")
                ) {
                    filter { eq("id", patientId) }
                }.decodeSingle<Patient>()
                
                currentPatient = patient
                displayPatientInfo(patient)

                   // Load medical records (now diagnosis)
                val records = SupabaseClient.client.from("diagnosis").select(
                    columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, cabang_package(*, packages(*)), profiles(*)")
                ) {
                    filter { eq("patient_id", patientId) }
                    order("id", Order.DESCENDING)
                }.decodeList<com.project.fisionettest.data.model.Diagnosis>()

                // Load packages for tools mapping
                val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
                val packages = SupabaseClient.getCabangPackagesForClinic(prefs.clinic)
                val packageToolsMap = packages.associate { (it.packages?.name ?: "") to (it.packages?.tools?.joinToString(", ") ?: "") }

                _binding?.let { b ->
                    diagnosisAdapter.submitList(records)
                    b.tvEmptyRecords.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                }
                
                // Load patient progress
                val progressList = SupabaseClient.client.from("patient_progress").select {
                    filter { eq("patient_id", patientId) }
                    order("id", Order.DESCENDING)
                }.decodeList<PatientProgress>()

                // Total Kunjungan = Diagnosa + Perkembangan Pasien
                val totalVisits = records.size + progressList.size
                _binding?.tvVisitCount?.text = "$totalVisits Kali"

                // Load transactions and count success status
                val transactions = SupabaseClient.client.from("transactions").select {
                    filter { eq("patient_id", patientId) }
                }.decodeList<Transaction>()

                val successTransactionsCount = transactions.count { it.payment_status == "success" }
                _binding?.tvTransactionCount?.text = "$successTransactionsCount Kali"

                // Update adapter with tools, tools-to-package, and status maps
                val toolsToPackageMap = packages.associate { (it.packages?.tools?.joinToString(", ") ?: "") to (it.packages?.name ?: "") }
                val diagnosisStatusMap = transactions.associate { it.diagnosis_id to (it.payment_status ?: "pending") }
                diagnosisAdapter.updateExtraData(packageToolsMap, diagnosisStatusMap, toolsToPackageMap)

                // Check for active/pending therapy transaction
                val pendingTrx = transactions.firstOrNull { it.payment_status == "pending" }
                if (pendingTrx != null) {
                    _binding?.let { b ->
                        b.cvActiveTherapy.visibility = View.VISIBLE
                        val activeDiag = records.firstOrNull { it.id == pendingTrx.diagnosis_id }
                        b.tvActiveDiagnosis.text = "Diagnosis: ${activeDiag?.diagnosa ?: "-"}"
                        val matchedPkg = packages.firstOrNull { it.id == pendingTrx.cabang_package_id }
                        val pkgName = matchedPkg?.packages?.name ?: "-"
                        b.tvActivePackage.text = "Paket Terapi: $pkgName"
                        val toolsStr = matchedPkg?.let { packageToolsMap[it.packages?.name] } ?: "-"
                        b.tvActiveTools?.text = "Alat Terapi: $toolsStr"
                        
                        b.btnPayActiveTherapy.setOnClickListener {
                            val bundle = Bundle().apply {
                                putInt("transactionId", pendingTrx.id ?: 0)
                            }
                            findNavController().navigate(R.id.action_patient_detail_to_receipt, bundle)
                        }
                    }
                } else {
                    _binding?.cvActiveTherapy?.visibility = View.GONE
                }
                
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _binding?.pbLoading?.visibility = View.GONE
            }
        }
    }

    private fun displayPatientInfo(patient: Patient) {
        binding.tvPatientName.text = "Nama: ${patient.name}"
        binding.tvPatientAge.text = "Umur: ${patient.umur ?: "-"} tahun"
        
        binding.tvPatientGender.text = "Jenis Kelamin: ${when(patient.gender) {
            "L" -> "Laki-laki"
            "P" -> "Perempuan"
            else -> "-"
        }}"
        binding.tvPatientPhone.text = "Telepon: ${patient.phone ?: "-"}"
        binding.tvPatientAddress.text = "Alamat: ${patient.address ?: "-"}"
        binding.tvPatientOccupation.text = "Pekerjaan: ${patient.pekerjaan ?: "-"}"
        binding.tvPatientTherapist.text = "Diinput oleh: ${patient.profiles?.displayName ?: "-"}"
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Pasien")
            .setMessage("Apakah Anda yakin ingin menghapus pasien ini? Semua rekam medis akan ikut terhapus.")
            .setPositiveButton("Hapus") { _, _ ->
                deletePatient()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePatient() {
        lifecycleScope.launch {
            try {
                // Delete references in appointments
                SupabaseClient.client.from("appointments").delete {
                     filter { eq("patient_id", patientId) }
                }

                // Delete medical records (diagnosis)
                SupabaseClient.client.from("diagnosis").delete {
                    filter { eq("patient_id", patientId) }
                }
                
                // Delete patient progress
                SupabaseClient.client.from("patient_progress").delete {
                    filter { eq("patient_id", patientId) }
                }

                // Delete transactions
                SupabaseClient.client.from("transactions").delete {
                    filter { eq("patient_id", patientId) }
                }
                
                // Delete patient
                SupabaseClient.client.from("patients").delete {
                    filter { eq("id", patientId) }
                }

                Toast.makeText(requireContext(), "Pasien berhasil dihapus", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddAppointmentDialog() {
        val patient = currentPatient ?: return
        val dialogBinding = com.project.fisionettest.databinding.DialogAddAppointmentBinding.inflate(layoutInflater)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        var selectedDate: String? = null
        var selectedTime: String? = null
        
        // Pre-fill patient name and disable changing it
        dialogBinding.actPatient.setText(patient.name)
        dialogBinding.tilPatient.isEnabled = false
        dialogBinding.actPatient.isEnabled = false

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

            if (selectedDate == null || selectedTime == null) {
                Toast.makeText(requireContext(), "Silakan isi tanggal dan waktu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val prefs = AppPreferences(requireContext())
                    val newAppointment = kotlinx.serialization.json.buildJsonObject {
                        put("patient_id", patient.id)
                        put("date", selectedDate!!)
                        put("time", selectedTime!!)
                        put("status", "Terjadwal")
                        put("notes", if (notes.isBlank()) null else notes)
                        // Auto-assign therapist via profile_id
                        val user = SupabaseClient.client.auth.currentUserOrNull()
                        put("profile_id", user?.id)
                    }
                    SupabaseClient.client.from("appointments").insert(newAppointment)
                    Toast.makeText(requireContext(), "Appointment berhasil dijadwalkan", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        loadPatientData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
