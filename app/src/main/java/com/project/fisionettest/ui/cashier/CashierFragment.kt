package com.project.fisionettest.ui.cashier

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Diagnosis
import com.project.fisionettest.data.model.Package
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.data.repository.XenditRepository
import com.project.fisionettest.databinding.FragmentCashierBinding
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CashierFragment : Fragment() {

    private var _binding: FragmentCashierBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: AppPreferences
    private val xenditRepository = XenditRepository()

    private var patientList: List<Patient>   = emptyList()
    private var diagnosisList: List<Diagnosis> = emptyList()
    private var packageList: List<com.project.fisionettest.data.model.CabangPackage>   = emptyList()

    private var selectedPatient: Patient?   = null
    private var selectedDiagnosis: Diagnosis? = null
    private var selectedPackage: com.project.fisionettest.data.model.CabangPackage?   = null
    private var selectedDate: String        = ""

    // ── Cabang otomatis dari profil terapis ─────────────────────────────
    private var assignedCabang: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCashierBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPreferences(requireContext())

        setupHeader()
        setupDatePicker()
        loadData()

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnSaveTransaction.setOnClickListener { saveTransactionAndPay() }
    }

    private fun setupHeader() {
        val name = prefs.userName ?: run {
            SupabaseClient.client.auth.currentUserOrNull()?.email?.substringBefore("@") ?: "Terapis"
        }
        binding.tvTherapistName.text = "Terapis: $name"

        // Tanggal default hari ini
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = dateFormat.format(Date())
        binding.etDate.setText(selectedDate)

        // ── Cabang otomatis — baca dari SharedPreferences ────────────────
        assignedCabang = prefs.clinic ?: "Cabang 1"
        binding.tvAutoCabang.text = assignedCabang
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                    binding.etDate.setText(selectedDate)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                // Load semua pasien (shared antar cabang)
                patientList = SupabaseClient.client.from("patients").select().decodeList()
                val patientAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    patientList.map { it.name }
                )
                binding.actPatient.setAdapter(patientAdapter)
                binding.actPatient.setOnItemClickListener { parent, _, position, _ ->
                    val selectedName = parent.getItemAtPosition(position).toString()
                    selectedPatient = patientList.find { it.name == selectedName }
                    selectedPatient?.let {
                        loadDiagnosisForPatient(it.id ?: 0)
                    }
                    selectedDiagnosis = null
                    binding.actDiagnosis.setText("")
                    binding.tilPatient.error = null
                }

                // Clean error on typing
                binding.actPatient.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        binding.tilPatient.error = null
                        // If user manual edits, clear selected patient until they select a valid one again
                        if (selectedPatient?.name != s.toString()) {
                            selectedPatient = null
                            selectedDiagnosis = null
                            binding.actDiagnosis.setText("")
                        }
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                })

                // Load paket
                packageList = SupabaseClient.getCabangPackagesForClinic(prefs.clinicId)
                val packageAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    packageList.map { "${it.packages?.name} (Alat: ${it.packages?.tools?.joinToString(", ")})" }
                )
                packageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerPackages.adapter = packageAdapter

                if (packageList.isNotEmpty()) {
                    selectedPackage = packageList[0]
                    updatePackageDisplay(selectedPackage!!)
                }

                binding.spinnerPackages.onItemSelectedListener =
                    object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selectedPackage = packageList.getOrNull(position)
                            selectedPackage?.let { updatePackageDisplay(it) }
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDiagnosisForPatient(patientId: Int) {
        lifecycleScope.launch {
            try {
                diagnosisList = SupabaseClient.client.from("diagnosis").select {
                    filter { eq("patient_id", patientId) }
                }.decodeList()

                val diagAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    diagnosisList.map { "${it.diagnosa} (${it.date})" }
                )
                binding.actDiagnosis.setAdapter(diagAdapter)
                binding.actDiagnosis.isEnabled = true
                binding.tilDiagnosis.isEnabled = true
                binding.actDiagnosis.setOnItemClickListener { parent, _, position, _ ->
                    val selectedDiagString = parent.getItemAtPosition(position).toString()
                    selectedDiagnosis = diagnosisList.find { "${it.diagnosa} (${it.date})" == selectedDiagString }
                    binding.tilDiagnosis.error = null
                }

                // Clean error on typing/changes
                binding.actDiagnosis.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        binding.tilDiagnosis.error = null
                        if (selectedDiagnosis != null && "${selectedDiagnosis!!.diagnosa} (${selectedDiagnosis!!.date})" != s.toString()) {
                            selectedDiagnosis = null
                        }
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                })
            } catch (e: Exception) {
                binding.actDiagnosis.isEnabled = false
            }
        }
    }

    private fun updatePackageDisplay(pkg: com.project.fisionettest.data.model.CabangPackage) {
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        val price = pkg.packages?.price ?: 0.0
        binding.tvPrice.text = formatter.format(price)
        binding.tvTools.text = "Alat: ${pkg.packages?.tools?.joinToString(", ") ?: "-"}"
    }

    private fun saveTransactionAndPay() {
        var isValid = true

        val patientText = binding.actPatient.text.toString().trim()
        if (patientText.isEmpty()) {
            binding.tilPatient.error = "Nama pasien tidak boleh kosong"
            isValid = false
        } else if (selectedPatient == null || selectedPatient?.name != patientText) {
            binding.tilPatient.error = "Pilih nama pasien yang valid dari daftar"
            isValid = false
        } else {
            binding.tilPatient.error = null
        }

        val diagnosisText = binding.actDiagnosis.text.toString().trim()
        if (diagnosisText.isEmpty()) {
            binding.tilDiagnosis.error = "Diagnosis tidak boleh kosong"
            isValid = false
        } else if (selectedDiagnosis == null || "${selectedDiagnosis!!.diagnosa} (${selectedDiagnosis!!.date})" != diagnosisText) {
            binding.tilDiagnosis.error = "Pilih diagnosis yang valid dari daftar"
            isValid = false
        } else {
            binding.tilDiagnosis.error = null
        }

        if (selectedPackage == null) {
            Toast.makeText(requireContext(), "Pilih paket layanan", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (selectedDate.isBlank()) {
            Toast.makeText(requireContext(), "Pilih tanggal transaksi", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (!isValid) {
            Toast.makeText(requireContext(), "Kolom yang kosong/tidak valid harus diisi terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveTransaction.isEnabled = false
        
        // Show progress dialog
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_payment_status)
            .setCancelable(false)
            .create()
        progressDialog.show()
        progressDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Customize status text for preparation phase
        progressDialog.findViewById<android.widget.TextView>(R.id.tv_status_title)?.text = "Memproses Transaksi"
        progressDialog.findViewById<android.widget.TextView>(R.id.tv_status_message)?.text = "Sedang menyiapkan invoice pembayaran..."

        lifecycleScope.launch {
            try {
                val user     = SupabaseClient.client.auth.currentUserOrNull()
                val pkg      = selectedPackage!!
                val userName = prefs.userName ?: user?.email?.substringBefore("@") ?: "Terapis"

                // LANGKAH 1: Simpan transaksi (cabang otomatis dari assignedCabang)
                val transactionData = buildJsonObject {
                    put("date", selectedDate)
                    put("patient_id", selectedPatient!!.id)
                    put("cabang_package_id", pkg.id)
                    selectedDiagnosis?.id?.let { put("diagnosis_id", it) }
                    put("total_amount", pkg.packages?.price ?: 0.0)
                    put("payment_status", "pending")
                    put("id_cabang", prefs.clinicId)
                    put("profile_id", user?.id)
                }

                val inserted = SupabaseClient.client
                    .from("transactions")
                    .insert(transactionData) { select() }
                    .decodeSingle<com.project.fisionettest.data.model.Transaction>()

                val transactionId = inserted.id
                    ?: throw Exception("Gagal mendapatkan ID transaksi")

                // LANGKAH 2: Buat invoice Xendit
                val description = "Pembayaran ${pkg.packages?.name} - ${selectedPatient!!.name} - $assignedCabang"
                val payerEmail  = user?.email ?: "patient@klikfisio.com"

                val xenditResult = xenditRepository.createInvoice(
                    transactionId = transactionId,
                    amount        = pkg.packages?.price ?: 0.0,
                    payerEmail    = payerEmail,
                    description   = description
                )

                // LANGKAH 3: Simpan xendit_id ke transaksi
                SupabaseClient.client.from("transactions").update(
                    buildJsonObject { put("xendit_id", xenditResult.invoiceId) }
                ) { filter { eq("id", transactionId) } }

                // Dismiss progress dialog before navigating
                progressDialog.dismiss()

                // LANGKAH 4: Buka halaman bayar Xendit di WebView internal
                val bundle = Bundle().apply {
                    putString("paymentUrl", xenditResult.invoiceUrl)
                    putInt("transactionId", transactionId)
                }
                findNavController().navigate(R.id.action_cashierFragment_to_paymentFragment, bundle)

            } catch (e: Exception) {
                // Update dialog to show error
                val ivStatusIcon = progressDialog.findViewById<android.widget.ImageView>(R.id.iv_status_icon)
                val pbLoading = progressDialog.findViewById<android.widget.ProgressBar>(R.id.pb_loading)
                val tvStatusTitle = progressDialog.findViewById<android.widget.TextView>(R.id.tv_status_title)
                val tvStatusMessage = progressDialog.findViewById<android.widget.TextView>(R.id.tv_status_message)
                val layoutActions = progressDialog.findViewById<android.view.View>(R.id.layout_actions)
                val btnToHistory = progressDialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_to_history)
                val btnToHome = progressDialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_to_home)

                pbLoading?.visibility = View.GONE
                ivStatusIcon?.visibility = View.VISIBLE
                ivStatusIcon?.setImageResource(android.R.drawable.ic_delete)
                ivStatusIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_absent)
                )
                tvStatusTitle?.text = "Transaksi Gagal"
                tvStatusMessage?.text = "Gagal memproses: ${e.message}"
                
                layoutActions?.visibility = View.VISIBLE
                btnToHistory?.visibility = View.GONE
                btnToHome?.text = "Tutup"
                btnToHome?.setOnClickListener {
                    progressDialog.dismiss()
                }
            } finally {
                binding.btnSaveTransaction.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
