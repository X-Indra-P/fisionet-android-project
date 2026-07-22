package com.project.fisionettest.ui.cashier

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Package
import com.project.fisionettest.data.model.Clinic
import com.project.fisionettest.data.model.Tool
import com.project.fisionettest.data.model.CabangPackage
import com.project.fisionettest.databinding.DialogAddPackageBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class AddPackageDialog(
    private val packageToEdit: Package? = null,
    private val onPackageSaved: () -> Unit
) : DialogFragment() {

    private var _binding: DialogAddPackageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddPackageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill data if editing
        if (packageToEdit != null) {
            binding.etPackageName.setText(packageToEdit.name)
            binding.etPackagePrice.setText(packageToEdit.price.toInt().toString())
            binding.tvTitle.text = "Edit Paket"
            binding.btnSavePackage.text = "Simpan Perubahan"
        }

        // Fetch tools and set checkbox states
        loadToolsAndSetup()

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSavePackage.setOnClickListener {
            savePackage()
        }
    }

    private fun loadToolsAndSetup() {
        binding.containerTools.removeAllViews()
        lifecycleScope.launch {
            try {
                // Ensure Clinics exist in "cabang" table
                val existingClinics = SupabaseClient.client.from("cabang").select().decodeList<Clinic>()
                if (existingClinics.isEmpty()) {
                    SupabaseClient.client.from("cabang").insert(listOf(
                        Clinic(id = 1, nama_cabang = "Cabang 1", alamat_cabang = "Alamat Cabang 1"),
                        Clinic(id = 2, nama_cabang = "Cabang 2", alamat_cabang = "Alamat Cabang 2")
                    ))
                }

                // Load all existing tools
                val tools = SupabaseClient.client.from("tools").select().decodeList<Tool>().sortedBy { it.nama_tools }

                // If packageToEdit is not null, load its mappings to pre-fill checkboxes
                val mappings = if (packageToEdit != null) {
                    SupabaseClient.client.from("cabang_package").select {
                        filter { eq("id_package", packageToEdit.id!!) }
                    }.decodeList<CabangPackage>()
                } else {
                    null
                }

                // Populate clinics checkboxes
                if (mappings != null) {
                    val clinicIds = mappings.map { it.id_cabang }.distinct()
                    binding.cbCabang1.isChecked = clinicIds.contains(1)
                    binding.cbCabang2.isChecked = clinicIds.contains(2)
                } else {
                    // Default check both for new package
                    binding.cbCabang1.isChecked = true
                    binding.cbCabang2.isChecked = true
                }

                val mappedToolIds = mappings?.flatMap { it.id_tools ?: emptyList() }?.distinct() ?: emptyList()

                if (tools.isEmpty()) {
                    val tvNoTools = android.widget.TextView(requireContext()).apply {
                        text = "Belum ada alat terdaftar. Kelola alat terlebih dahulu di halaman Paket."
                        setTextColor(resources.getColor(R.color.text_secondary, null))
                        textSize = 14f
                        setPadding(0, 8, 0, 8)
                    }
                    binding.containerTools.addView(tvNoTools)
                } else {
                    tools.forEach { tool ->
                        val checkBox = CheckBox(requireContext()).apply {
                            text = tool.nama_tools
                            tag = tool.id
                            isChecked = mappedToolIds.contains(tool.id)
                            setTextColor(resources.getColor(R.color.black, null))
                            textSize = 15f
                            setPadding(8, 8, 8, 8)
                        }
                        binding.containerTools.addView(checkBox)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memuat alat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePackage() {
        val name = binding.etPackageName.text.toString().trim()
        val priceStr = binding.etPackagePrice.text.toString().trim()
        
        if (name.isBlank() || priceStr.isBlank()) {
            Toast.makeText(requireContext(), "Nama dan Harga harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null) {
            Toast.makeText(requireContext(), "Harga tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val isCabang1Selected = binding.cbCabang1.isChecked
        val isCabang2Selected = binding.cbCabang2.isChecked

        if (!isCabang1Selected && !isCabang2Selected) {
            Toast.makeText(requireContext(), "Pilih minimal satu Cabang Penugasan", Toast.LENGTH_SHORT).show()
            return
        }

        // Collect checked tool IDs
        val checkedToolIds = mutableListOf<Int>()
        for (i in 0 until binding.containerTools.childCount) {
            val child = binding.containerTools.getChildAt(i)
            if (child is CheckBox && child.isChecked) {
                val toolId = child.tag as? Int
                if (toolId != null) {
                    checkedToolIds.add(toolId)
                }
            }
        }

        lifecycleScope.launch {
            binding.btnSavePackage.isEnabled = false
            try {
                // 1. Save / Update Package
                val packageId = if (packageToEdit == null) {
                    val newPkg = Package(name = name, price = price)
                    val insertedPkg = SupabaseClient.client.from("packages").insert(newPkg) {
                        select()
                    }.decodeSingle<Package>()
                    insertedPkg.id!!
                } else {
                    val updatedPkg = Package(id = packageToEdit.id, name = name, price = price)
                    SupabaseClient.client.from("packages").update(updatedPkg) {
                        filter { eq("id", packageToEdit.id!!) }
                    }
                    // Delete old mappings for this package first
                    SupabaseClient.client.from("cabang_package").delete {
                        filter { eq("id_package", packageToEdit.id!!) }
                    }
                    packageToEdit.id!!
                }

                // 2. Create Clinic-Package mappings
                val clinicsToMap = mutableListOf<Int>()
                if (isCabang1Selected) clinicsToMap.add(1)
                if (isCabang2Selected) clinicsToMap.add(2)

                val mappings = clinicsToMap.map { clinicId ->
                    CabangPackage(
                        id_cabang = clinicId,
                        id_package = packageId,
                        id_tools = checkedToolIds
                    )
                }

                SupabaseClient.client.from("cabang_package").insert(mappings)

                Toast.makeText(requireContext(), "Paket berhasil disimpan", Toast.LENGTH_SHORT).show()
                onPackageSaved()
                dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal menyimpan paket: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSavePackage.isEnabled = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
