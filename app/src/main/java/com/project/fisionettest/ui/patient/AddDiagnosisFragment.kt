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
import com.project.fisionettest.data.model.Diagnosis
import com.project.fisionettest.databinding.FragmentAddDiagnosisBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*

class AddDiagnosisFragment : Fragment() {
    private var _binding: FragmentAddDiagnosisBinding? = null
    private val binding get() = _binding!!
    private var patientId: Int = 0
    private var selectedDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDiagnosisBinding.inflate(inflater, container, false)
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
        
        loadExistingDiagnoses()

        // Toggle Inspection Input
        binding.tilInspection.visibility = View.GONE // Default hidden
        binding.cbHasInspection.isChecked = false

        binding.cbHasInspection.setOnCheckedChangeListener { _, isChecked ->
            binding.tilInspection.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                binding.etInspection.text?.clear()
            }
        }

        binding.btnSave.setOnClickListener {
            saveDiagnosis()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadExistingDiagnoses() {
        lifecycleScope.launch {
            try {
                // Fetch all diagnoses to get unique names. 
                // Use JsonObject to avoid MissingFieldException because we only select 'diagnosa'.
                val diagnoses = SupabaseClient.client.from("diagnosis")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("diagnosa"))
                    .decodeList<kotlinx.serialization.json.JsonObject>()
                
                // Extract 'diagnosa' string from JsonObject
                val distinctDiagnoses = diagnoses.mapNotNull { 
                    it["diagnosa"]?.toString()?.replace("\"", "") 
                }.filter { it.isNotBlank() }.distinct().sorted()
                
                if (distinctDiagnoses.isNotEmpty()) {
                    val adapter = android.widget.ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        distinctDiagnoses
                    )
                    binding.etDiagnosis.setAdapter(adapter)
                }
            } catch (e: Exception) {
                // Silent fail for autocomplete suggestions is acceptable, but log likely needed if debugging
                e.printStackTrace()
            }
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

    private fun saveDiagnosis() {
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
                // Use JsonObject to avoid sending null 'id' and 'created_at' and to be safe
                val newRecord = kotlinx.serialization.json.buildJsonObject {
                    put("patient_id", patientId)
                    put("date", selectedDate)
                    put("diagnosa", diagnosis)
                    put("vital_sign", vitalSign)
                    put("patient_problem", patientProblem)
                    put("inspection", inspection)
                    put("planning", planning)
                }

                SupabaseClient.client.from("diagnosis").insert(newRecord) // Updated table name
                Toast.makeText(requireContext(), "Diagnosis berhasil ditambahkan", Toast.LENGTH_SHORT).show()
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
