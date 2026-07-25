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
import com.project.fisionettest.data.model.Diagnosis
import com.project.fisionettest.databinding.FragmentDiagnosisDetailBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Calendar
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.gotrue.auth

class DiagnosisDetailFragment : Fragment() {

    private var _binding: FragmentDiagnosisDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressAdapter: PatientProgressAdapter
    private var patientId: Int = 0
    private var currentRecord: Diagnosis? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiagnosisDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var isEditing = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recordJson = arguments?.getString("diagnosis")
        if (recordJson != null) {
            val record = Json.decodeFromString(com.project.fisionettest.data.model.Diagnosis.serializer(), recordJson)
            currentRecord = record
            displayRecord(record)
            patientId = record.patient_id
            
            setupProgressRecyclerView()
            loadProgressData()
        }

        val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
        if (prefs.userRole == 2) {
            binding.btnEditRecord.visibility = View.GONE
            binding.btnDeleteRecord.visibility = View.GONE
        } else {
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

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        if (prefs.userRole == 1) {
            binding.btnServeSession.visibility = View.GONE
        }

        binding.btnServeSession.setOnClickListener {
            showServeSessionDialog()
        }
    }

    private fun displayRecord(record: com.project.fisionettest.data.model.Diagnosis) {
        binding.tvDetailDate.text = "Tanggal: ${record.date}"
        
        // Read Mode (TextViews)
        binding.tvReadDiagnosis.text = record.diagnosa
        binding.tvReadVitalSign.text = record.vital_sign
        binding.tvReadPatientProblem.text = record.patient_problem
        binding.tvReadInspection.text = record.inspection
        
        val packageTools = record.cabang_package?.packages?.tools?.joinToString(", ") ?: "-"
        binding.tvReadPlanning.text = packageTools

        // Edit Mode (EditTexts)
        binding.etDetailDiagnosis.setText(record.diagnosa)
        binding.etDetailVitalSign.setText(record.vital_sign)
        binding.etDetailPatientProblem.setText(record.patient_problem)
        binding.etDetailInspection.setText(record.inspection)
        binding.etDetailPlanning.setText(packageTools)
        // Make planning read-only in edit mode since it's driven by package selection now
        binding.etDetailPlanning.isEnabled = false
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
                    diagnosa = binding.etDetailDiagnosis.text.toString(), // Updated field name
                    vital_sign = binding.etDetailVitalSign.text.toString(),
                    patient_problem = binding.etDetailPatientProblem.text.toString(),
                    inspection = binding.etDetailInspection.text.toString()
                )

                if (updatedRecord != null) {
                    SupabaseClient.client.from("diagnosis").update(updatedRecord) { // Table name changed
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
                    SupabaseClient.client.from("diagnosis").delete { // Table name changed
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
            val initialBinding = _binding ?: return@launch
            val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
            initialBinding.pbLoading.visibility = View.VISIBLE
            initialBinding.tvEmptyProgress.visibility = View.GONE
            try {
                // 1. Fetch transactions for this diagnosis to check for active/pending session
                val transactions = SupabaseClient.client.from("transactions").select {
                    filter {
                        eq("patient_id", patientId)
                        eq("diagnosis_id", currentRecord?.id ?: -1)
                    }
                }.decodeList<com.project.fisionettest.data.model.Transaction>()

                val pendingTrx = transactions.firstOrNull { it.payment_status == "pending" }

                val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
                val packages = SupabaseClient.getCabangPackagesForClinic(prefs.clinicId)
                val packageToolsMap = packages.associate { (it.packages?.name ?: "") to (it.packages?.tools?.joinToString(", ") ?: "") }

                 // 3. Fetch progress records for this diagnosis
                 val progressList = SupabaseClient.client.from("patient_progress").select(
                     columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, profiles(*), cabang_package(*, packages(*)), cabang(*)")
                 ) {
                     filter {
                          eq("patient_id", patientId)
                          currentRecord?.id?.let { eq("diagnosis_id", it) }
                     }
                     order("id", Order.DESCENDING)
                 }.decodeList<com.project.fisionettest.data.model.PatientProgress>()
 
                 val allTools = SupabaseClient.client.from("tools").select().decodeList<com.project.fisionettest.data.model.Tool>()
                 val toolsMap = allTools.filter { it.id != null }.associate { it.id!! to (it.nama_tools ?: "") }
 
                 // Check again after suspend points
                 val uiBinding = _binding ?: return@launch
                 progressAdapter.updateToolsMap(toolsMap)

                // Show tools instead of package name in Planning
                currentRecord?.let { record ->
                    val pkgName = record.cabang_package?.packages?.name
                    val toolsForPlanning = if (pkgName != null) packageToolsMap[pkgName] ?: "-" else "-"
                    uiBinding.tvReadPlanning.text = toolsForPlanning
                    uiBinding.etDetailPlanning.setText(toolsForPlanning)
                }

                // 4. Determine display depending on pending transactions
                if (pendingTrx != null) {
                    uiBinding.btnServeSession.visibility = View.GONE
                    uiBinding.cvActiveSession.visibility = View.VISIBLE

                    // Find corresponding package name
                    val matchedPkg = packages.firstOrNull { it.id == pendingTrx.cabang_package_id }
                    val activePackageName = matchedPkg?.packages?.name ?: ""

                    uiBinding.tvActiveDate.text = "Tanggal Sesi: ${pendingTrx.date}"
                    uiBinding.tvActivePackageName.text = "Paket Terapi: $activePackageName"
                    uiBinding.tvActiveTools.text = "Alat Terapi: ${packageToolsMap[activePackageName] ?: "-"}"

                    // Find progress record for this active transaction
                    val activeProgress = progressList.firstOrNull { 
                        it.date == pendingTrx.date && it.cabang_package?.id == pendingTrx.cabang_package_id 
                    }

                    uiBinding.btnPayActiveSession.setOnClickListener {
                        showAddProgressBeforePaymentDialog(activeProgress?.id, activeProgress?.progress_note, pendingTrx.id ?: 0)
                    }

                    // Display only completed sessions in history
                    val completedProgressList = progressList.filter { it.id != activeProgress?.id }
                    progressAdapter.submitList(completedProgressList)
                    uiBinding.tvEmptyProgress.visibility = if (completedProgressList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    if (prefs.userRole == 1) {
                        uiBinding.btnServeSession.visibility = View.GONE
                    } else {
                        uiBinding.btnServeSession.visibility = View.VISIBLE
                    }
                    uiBinding.cvActiveSession.visibility = View.GONE
                    
                    progressAdapter.submitList(progressList)
                    uiBinding.tvEmptyProgress.visibility = if (progressList.isEmpty()) View.VISIBLE else View.GONE
                }

            } catch (e: Exception) {
                 e.printStackTrace()
            } finally {
                _binding?.pbLoading?.visibility = View.GONE
            }
        }
    }

    private fun showServeSessionDialog() {
        lifecycleScope.launch {
            val initialBinding = _binding ?: return@launch
            initialBinding.pbLoading.visibility = View.VISIBLE
            try {
                val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
                val packageList = SupabaseClient.getCabangPackagesForClinic(prefs.clinicId)
                val packageNames = packageList.map { pkg ->
                    "${pkg.packages?.name} - Alat: ${pkg.packages?.tools?.joinToString(", ")}"
                }.toTypedArray()

                var selectedIndex = 0
                AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Paket Terapi (Layani)")
                    .setSingleChoiceItems(packageNames, 0) { _, which ->
                        selectedIndex = which
                    }
                    .setPositiveButton("Layani") { dialog, _ ->
                        dialog.dismiss()
                        startTherapySession(packageList[selectedIndex])
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat paket: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _binding?.pbLoading?.visibility = View.GONE
            }
        }
    }

    private fun startTherapySession(selectedPkg: com.project.fisionettest.data.model.CabangPackage) {
        lifecycleScope.launch {
            val initialBinding = _binding ?: return@launch
            initialBinding.pbLoading.visibility = View.VISIBLE
            try {
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
                val user = SupabaseClient.client.auth.currentUserOrNull()

                // 1. Create Transaction (pending)
                val newTransaction = kotlinx.serialization.json.buildJsonObject {
                    put("date", todayStr)
                    put("patient_id", patientId)
                    put("diagnosis_id", currentRecord?.id)
                    put("cabang_package_id", selectedPkg.id)
                    put("total_amount", selectedPkg.packages?.price ?: 0.0)
                    put("payment_status", "pending")
                    put("id_cabang", prefs.clinicId)
                    put("profile_id", user?.id)
                }
                SupabaseClient.client.from("transactions").insert(newTransaction)

                // 2. Create Patient Progress record
                val newProgress = com.project.fisionettest.data.model.PatientProgress(
                    patient_id = patientId,
                    diagnosis_id = currentRecord?.id,
                    date = todayStr,
                    progress_note = "",
                    cabang_package_id = selectedPkg.id,
                    status = "Proses",
                    profile_id = prefs.userId,
                    id_cabang = prefs.clinicId
                )
                SupabaseClient.client.from("patient_progress").insert(newProgress)

                // 3. Update Diagnosis status back to "Proses"
                currentRecord?.id?.let { diagId ->
                    SupabaseClient.client.from("diagnosis").update(
                        buildJsonObject { put("status", "Proses") }
                    ) {
                        filter { eq("id", diagId) }
                    }
                }

                Toast.makeText(requireContext(), "Sesi terapi baru dimulai!", Toast.LENGTH_SHORT).show()
                loadProgressData() // Reload
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memulai sesi: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _binding?.pbLoading?.visibility = View.GONE
            }
        }
    }



    private fun showAddProgressBeforePaymentDialog(progressId: Int?, progressNote: String?, transactionId: Int) {
        val input = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "Tulis Catatan Perkembangan Sesi Ini..."
            minLines = 3
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            setText(progressNote ?: "")
        }
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            addView(input)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Catatan Perkembangan")
            .setMessage("Silakan lengkapi catatan perkembangan pasien sebelum melanjutkan ke pembayaran.")
            .setView(layout)
            .setPositiveButton("Simpan & Lanjutkan") { _, _ ->
                val noteText = input.text.toString()
                if (noteText.isBlank()) {
                    Toast.makeText(requireContext(), "Catatan perkembangan harus diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveActiveProgressNoteBeforePayment(progressId, noteText, transactionId)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveActiveProgressNoteBeforePayment(progressId: Int?, note: String, transactionId: Int) {
        lifecycleScope.launch {
            val initialBinding = _binding ?: return@launch
            initialBinding.pbLoading.visibility = View.VISIBLE
            try {
                SupabaseClient.client.from("patient_progress").update(
                    buildJsonObject {
                        put("progress_note", note)
                    }
                ) {
                    filter { eq("id", progressId ?: -1) }
                }
                Toast.makeText(requireContext(), "Catatan perkembangan disimpan", Toast.LENGTH_SHORT).show()
                
                // Navigate to Receipt page
                val bundle = Bundle().apply {
                    putInt("transactionId", transactionId)
                }
                findNavController().navigate(R.id.receiptFragment, bundle)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menyimpan catatan: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _binding?.pbLoading?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
