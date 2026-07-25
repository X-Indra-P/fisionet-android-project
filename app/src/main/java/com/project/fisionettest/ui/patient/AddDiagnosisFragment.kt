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
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Diagnosis
import com.project.fisionettest.databinding.FragmentAddDiagnosisBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.gotrue.auth

class AddDiagnosisFragment : Fragment() {
    private var _binding: FragmentAddDiagnosisBinding? = null
    private val binding get() = _binding!!
    private var patientId: Int = 0
    private var selectedDate: String = ""

    private var packageList = listOf<com.project.fisionettest.data.model.CabangPackage>()
    private var selectedPackage: com.project.fisionettest.data.model.CabangPackage? = null

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
        loadPackages()

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

    private fun loadPackages() {
        lifecycleScope.launch {
            try {
                val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
                val packages = SupabaseClient.getCabangPackagesForClinic(prefs.clinicId)
                packageList = packages
                val names = packages.map { "${it.packages?.name} - Alat: ${it.packages?.tools?.joinToString(", ")}" }
                val adapter = android.widget.ArrayAdapter<String>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names.toTypedArray()
                )
                binding.etPlanning.setAdapter(adapter)
                
                binding.etPlanning.setOnItemClickListener { _, _, position, _ ->
                    selectedPackage = packageList[position]
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat paket: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                
                val uiBinding = _binding ?: return@launch
                if (distinctDiagnoses.isNotEmpty()) {
                    val adapter = android.widget.ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        distinctDiagnoses
                    )
                    uiBinding.etDiagnosis.setAdapter(adapter)
                }
            } catch (e: Exception) {
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

        if (diagnosis.isBlank()) {
            Toast.makeText(requireContext(), "Diagnosis harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPackage == null) {
            Toast.makeText(requireContext(), "Pilih Paket Terapi (Planning) terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val prefs = AppPreferences(requireContext())
                // Use JsonObject to avoid sending null 'id' and 'created_at' and to be safe
                val newRecord = kotlinx.serialization.json.buildJsonObject {
                    put("patient_id", patientId)
                    put("date", selectedDate)
                    put("diagnosa", diagnosis)
                    put("vital_sign", vitalSign)
                    put("patient_problem", patientProblem)
                    put("inspection", inspection)
                    put("profile_id", prefs.userId)
                    put("id_cabang", prefs.clinicId)
                    put("cabang_package_id", selectedPackage?.id)
                    put("status", "Proses")
                }

                // Insert diagnosis and get the returned model to grab the ID
                val insertedDiagnosis = SupabaseClient.client.from("diagnosis").insert(newRecord) {
                    select()
                }.decodeSingle<Diagnosis>()

                // Auto-create transaction with payment_status = pending
                val user = SupabaseClient.client.auth.currentUserOrNull()
                val newTransaction = kotlinx.serialization.json.buildJsonObject {
                    put("date", selectedDate)
                    put("patient_id", patientId)
                    put("diagnosis_id", insertedDiagnosis.id)
                    put("cabang_package_id", selectedPackage?.id)
                    put("total_amount", selectedPackage?.packages?.price ?: 0.0)
                    put("payment_status", "pending")
                    put("id_cabang", prefs.clinicId)
                    put("profile_id", user?.id)
                }
                SupabaseClient.client.from("transactions").insert(newTransaction)

                // Show success dialog
                val successDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                   .setView(R.layout.dialog_payment_status)
                   .setCancelable(false)
                   .create()
                successDialog.show()
                successDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                val pbLoading = successDialog.findViewById<android.widget.ProgressBar>(R.id.pb_loading)
                val ivStatusIcon = successDialog.findViewById<android.widget.ImageView>(R.id.iv_status_icon)
                val tvStatusTitle = successDialog.findViewById<android.widget.TextView>(R.id.tv_status_title)
                val tvStatusMessage = successDialog.findViewById<android.widget.TextView>(R.id.tv_status_message)

                pbLoading?.visibility = View.GONE
                ivStatusIcon?.visibility = View.VISIBLE
                ivStatusIcon?.setImageResource(android.R.drawable.checkbox_on_background)
                ivStatusIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_present)
                )
                tvStatusTitle?.text = "Diagnosis Berhasil Ditambahkan"
                tvStatusMessage?.text = "Mengalihkan ke detail pasien..."

                // Delay for 3 seconds
                kotlinx.coroutines.delay(3000)
                successDialog.dismiss()

                val hasPatientDetailInBackStack = try {
                    findNavController().getBackStackEntry(R.id.patientDetailFragment)
                    true
                } catch (e: Exception) {
                    false
                }

                if (hasPatientDetailInBackStack) {
                    findNavController().popBackStack(R.id.patientDetailFragment, false)
                } else {
                    val bundle = Bundle().apply {
                        putInt("patientId", patientId)
                    }
                    findNavController().navigate(
                        R.id.action_addDiagnosisFragment_to_patientDetailFragment,
                        bundle,
                        androidx.navigation.navOptions {
                            popUpTo(R.id.addDiagnosisFragment) {
                                inclusive = true
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _binding?.btnSave?.isEnabled = true
                _binding?.progressBar?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
