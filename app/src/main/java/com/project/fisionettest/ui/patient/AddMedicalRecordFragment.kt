package com.project.fisionettest.ui.patient

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController

import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.MedicalRecord
import com.project.fisionettest.databinding.FragmentAddMedicalRecordBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddMedicalRecordFragment : Fragment() {
    private var _binding: FragmentAddMedicalRecordBinding? = null
    private val binding get() = _binding!!
    private var patientId: Int = 0
    private var selectedDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMedicalRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        patientId = arguments?.getInt("patientId") ?: 0

        // Set default date to today
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = dateFormat.format(Date())
        binding.etDate.setText(selectedDate)

        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            saveMedicalRecord()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            binding.etDate.setText(selectedDate)
        }, year, month, day).show()
    }

    private fun saveMedicalRecord() {
        val diagnosis = binding.etDiagnosis.text.toString()
        val vitalSign = binding.etVitalSign.text.toString()
        val patientProblem = binding.etPatientProblem.text.toString()
        val inspection = binding.etInspection.text.toString()
        val planning = binding.etPlanning.text.toString()

        if (diagnosis.isBlank()) {
            Toast.makeText(requireContext(), "Diagnosis harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val newRecord = MedicalRecord(
                    patient_id = patientId,
                    date = selectedDate,
                    diagnosis = diagnosis,
                    vital_sign = vitalSign,
                    patient_problem = patientProblem,
                    inspection = inspection,
                    planning = planning
                )

                SupabaseClient.client.from("medical_records").insert(newRecord)
                Toast.makeText(requireContext(), "Rekam medis berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
