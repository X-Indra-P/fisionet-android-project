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
import com.project.fisionettest.data.model.MedicalRecord
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.databinding.FragmentPatientDetailBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.util.Calendar

class PatientDetailFragment : Fragment() {
    private var _binding: FragmentPatientDetailBinding? = null
    private val binding get() = _binding!!
    private var patientId: Int = 0
    private lateinit var medicalRecordAdapter: MedicalRecordAdapter
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
            findNavController().navigate(R.id.action_patient_detail_to_add_medical_record, bundle)
        }
    }

    private fun setupRecyclerView() {
        medicalRecordAdapter = MedicalRecordAdapter()
        binding.rvMedicalRecords.apply {
            adapter = medicalRecordAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadPatientData() {
        lifecycleScope.launch {
            try {
                // Load patient
                val patient = SupabaseClient.client.from("patients").select {
                    filter { eq("id", patientId) }
                }.decodeSingle<Patient>()
                
                currentPatient = patient
                displayPatientInfo(patient)

                // Load medical records
                val records = SupabaseClient.client.from("medical_records").select {
                    filter { eq("patient_id", patientId) }
                }.decodeList<MedicalRecord>()

                medicalRecordAdapter.submitList(records)
                binding.tvEmptyRecords.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayPatientInfo(patient: Patient) {
        binding.tvPatientName.text = "Nama: ${patient.name}"
        
        // Calculate age from date_of_birth if available
        val age = patient.date_of_birth?.let { calculateAge(it) } ?: "N/A"
        binding.tvPatientAge.text = "Umur: $age tahun"
        
        binding.tvPatientGender.text = "Jenis Kelamin: ${when(patient.gender) {
            "L" -> "Laki-laki"
            "P" -> "Perempuan"
            else -> "-"
        }}"
        binding.tvPatientPhone.text = "Telepon: ${patient.phone ?: "-"}"
        binding.tvPatientAddress.text = "Alamat: ${patient.address ?: "-"}"
        binding.tvPatientDiagnosis.text = "Diagnosis: ${patient.diagnosis}"
    }

    private fun calculateAge(dateOfBirth: String): Int {
        return try {
            val parts = dateOfBirth.split("-")
            val birthYear = parts[0].toInt()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            currentYear - birthYear
        } catch (e: Exception) {
            0
        }
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
                // Delete medical records first
                SupabaseClient.client.from("medical_records").delete {
                    filter { eq("patient_id", patientId) }
                }
                
                // Then delete patient
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

    override fun onResume() {
        super.onResume()
        loadPatientData() // Refresh when returning from edit
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
