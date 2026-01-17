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
import com.project.fisionettest.databinding.FragmentPatientDetailBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
            try {
                // Load patient
                val patient = SupabaseClient.client.from("patients").select {
                    filter { eq("id", patientId) }
                }.decodeSingle<Patient>()
                
                currentPatient = patient
                displayPatientInfo(patient)

                // Load medical records (now diagnosis)
                val records = SupabaseClient.client.from("diagnosis").select {
                    filter { eq("patient_id", patientId) }
                }.decodeList<com.project.fisionettest.data.model.Diagnosis>()

                diagnosisAdapter.submitList(records)
                binding.tvEmptyRecords.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        loadPatientData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
