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
import com.project.fisionettest.databinding.FragmentMedicalRecordDetailBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Calendar

class MedicalRecordDetailFragment : Fragment() {

    private var _binding: FragmentMedicalRecordDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressAdapter: PatientProgressAdapter
    private var patientId: Int = 0
    private var currentRecord: MedicalRecord? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicalRecordDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var isEditing = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recordJson = arguments?.getString("medicalRecord")
        if (recordJson != null) {
            val record = Json.decodeFromString(MedicalRecord.serializer(), recordJson)
            currentRecord = record
            displayRecord(record)
            patientId = record.patient_id
            
            setupProgressRecyclerView()
            loadProgressData()
        }

        binding.btnAddProgress.setOnClickListener {
            showAddProgressDialog()
        }

        binding.btnEditRecord.setOnClickListener {
            if (isEditing) {
                saveRecordChanges()
            } else {
                enableEditing(true)
            }
        }

        binding.btnDeleteRecord.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun displayRecord(record: MedicalRecord) {
        binding.tvDetailDate.text = "Tanggal: ${record.date}"
        
        // Read Mode (TextViews)
        binding.tvReadDiagnosis.text = record.diagnosis
        binding.tvReadVitalSign.text = record.vital_sign
        binding.tvReadPatientProblem.text = record.patient_problem
        binding.tvReadInspection.text = record.inspection
        binding.tvReadPlanning.text = record.planning

        // Edit Mode (EditTexts)
        binding.etDetailDiagnosis.setText(record.diagnosis)
        binding.etDetailVitalSign.setText(record.vital_sign)
        binding.etDetailPatientProblem.setText(record.patient_problem)
        binding.etDetailInspection.setText(record.inspection)
        binding.etDetailPlanning.setText(record.planning)
    }

    private fun enableEditing(enable: Boolean) {
        isEditing = enable
        
        if (enable) {
            // Switch to Edit Mode
            binding.layoutReadMode.visibility = View.GONE
            binding.layoutEditMode.visibility = View.VISIBLE
            
            binding.btnEditRecord.text = "Simpan"
            binding.btnEditRecord.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light, null))
            binding.btnEditRecord.setTextColor(resources.getColor(android.R.color.white, null))
        } else {
            // Switch to Read Mode (Default)
            binding.layoutReadMode.visibility = View.VISIBLE
            binding.layoutEditMode.visibility = View.GONE
            
            binding.btnEditRecord.text = "Edit"
             // Revert style
            binding.btnEditRecord.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            binding.btnEditRecord.setTextColor(resources.getColor(R.color.black, null))
        }
    }

    private fun saveRecordChanges() {
        lifecycleScope.launch {
            try {
                val updatedRecord = currentRecord?.copy(
                    diagnosis = binding.etDetailDiagnosis.text.toString(),
                    vital_sign = binding.etDetailVitalSign.text.toString(),
                    patient_problem = binding.etDetailPatientProblem.text.toString(),
                    inspection = binding.etDetailInspection.text.toString(),
                    planning = binding.etDetailPlanning.text.toString()
                )

                if (updatedRecord != null) {
                    SupabaseClient.client.from("medical_records").update(updatedRecord) {
                        filter { eq("id", updatedRecord.id ?: -1) }
                    }
                    Toast.makeText(requireContext(), "Perubahan berhasil disimpan", Toast.LENGTH_SHORT).show()
                    
                    // Update current record and refresh display
                    currentRecord = updatedRecord
                    displayRecord(updatedRecord)
                    
                    enableEditing(false)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // ... setupProgressRecyclerView, loadProgressData, showAddProgressDialog, saveProgress (keep these)

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Diagnosis")
            .setMessage("Apakah Anda yakin ingin menghapus data diagnosis ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteRecord()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteRecord() {
        lifecycleScope.launch {
            try {
                currentRecord?.id?.let { recordId ->
                    SupabaseClient.client.from("medical_records").delete {
                        filter { eq("id", recordId) }
                    }
                    Toast.makeText(requireContext(), "Diagnosis berhasil dihapus", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupProgressRecyclerView() {
        progressAdapter = PatientProgressAdapter()
        binding.rvPatientProgress.apply {
            adapter = progressAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadProgressData() {
        lifecycleScope.launch {
            try {
                val progressList = SupabaseClient.client.from("patient_progress").select {
                    filter { eq("patient_id", patientId) }
                    order("date", Order.DESCENDING)
                }.decodeList<com.project.fisionettest.data.model.PatientProgress>()
                
                progressAdapter.submitList(progressList)
                binding.tvEmptyProgress.visibility = if (progressList.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                 // Log error
            }
        }
    }

    private fun showAddProgressDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_progress, null)
        val etDate = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_progress_date)
        val etNote = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_progress_note)

        // Set current date by default
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        etDate.setText("$year-${month + 1}-$day")
        
        etDate.setOnClickListener {
             android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
                etDate.setText("$y-${m + 1}-$d")
            }, year, month, day).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Perkembangan")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val date = etDate.text.toString()
                val note = etNote.text.toString()
                if (date.isNotBlank() && note.isNotBlank()) {
                    saveProgress(date, note)
                } else {
                    Toast.makeText(requireContext(), "Mohon isi semua data", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveProgress(date: String, note: String) {
        lifecycleScope.launch {
            try {
                val newProgress = com.project.fisionettest.data.model.PatientProgress(
                    patient_id = patientId,
                    date = date,
                    progress_note = note
                )
                
                SupabaseClient.client.from("patient_progress").insert(newProgress)
                Toast.makeText(requireContext(), "Perkembangan berhasil disimpan", Toast.LENGTH_SHORT).show()
                loadProgressData() // Refresh list
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
