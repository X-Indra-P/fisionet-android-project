package com.project.fisionettest.ui.cashier

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.data.model.Package
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.databinding.FragmentCashierBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class CashierFragment : Fragment() {

    private var _binding: FragmentCashierBinding? = null
    private val binding get() = _binding!!

    private var selectedPatient: Patient? = null
    private var selectedPackage: Package? = null
    private var patientList = listOf<Patient>()
    private var packageList = listOf<Package>()

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

        setupDatePicker()
        loadPatients()
        loadPackages()

        binding.btnCreatePackage.setOnClickListener {
            showAddPackageDialog()
        }

        binding.btnSaveTransaction.setOnClickListener {
            saveTransaction()
        }

        binding.btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.action_cashier_to_history)
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        updateDateLabel(calendar) // Set default to today

        binding.etDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    updateDateLabel(calendar)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateLabel(calendar: Calendar) {
        val myFormat = "yyyy-MM-dd"
        val sdf = java.text.SimpleDateFormat(myFormat, Locale.US)
        binding.etDate.setText(sdf.format(calendar.time))
    }

    private fun loadPatients() {
        lifecycleScope.launch {
            try {
                val patients = SupabaseClient.client.from("patients").select().decodeList<Patient>()
                patientList = patients
                setupPatientSpinner()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat pasien: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPatientSpinner() {
        val patientNames = patientList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, patientNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPatients.adapter = adapter

        binding.spinnerPatients.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPatient = patientList.getOrNull(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadPackages() {
        lifecycleScope.launch {
            try {
                val packages = SupabaseClient.client.from("packages").select().decodeList<Package>()
                packageList = packages
                setupPackageSpinner()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat paket: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPackageSpinner() {
        val packageNames = packageList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, packageNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPackages.adapter = adapter

        binding.spinnerPackages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
             override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPackage = packageList.getOrNull(position)
                updatePackageDetails()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updatePackageDetails() {
        selectedPackage?.let { pkg ->
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            binding.tvPrice.text = "Harga: ${format.format(pkg.price)}"
            binding.tvTools.text = if (pkg.tools.isNotEmpty()) pkg.tools.joinToString(", ") else "-"
        }
    }

    private fun showAddPackageDialog() {
        val dialog = AddPackageDialog {
            loadPackages() // Refresh list after adding
        }
        dialog.show(parentFragmentManager, "AddPackageDialog")
    }

    private fun saveTransaction() {
        val date = binding.etDate.text.toString()
        if (selectedPatient == null || selectedPackage == null) {
            Toast.makeText(requireContext(), "Pilih Pasien dan Paket terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        binding.loadingBar.visibility = View.VISIBLE
        binding.btnSaveTransaction.isEnabled = false

        lifecycleScope.launch {
            try {
                val transaction = Transaction(
                    date = date,
                    patient_id = selectedPatient?.id,
                    patient_name = selectedPatient!!.name,
                    package_id = selectedPackage?.id,
                    package_name = selectedPackage!!.name,
                    amount = selectedPackage!!.price
                )

                SupabaseClient.client.from("transactions").insert(transaction)
                Toast.makeText(requireContext(), "Transaksi Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                // Optional: Clear selection or navigate
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.loadingBar.visibility = View.GONE
                binding.btnSaveTransaction.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
