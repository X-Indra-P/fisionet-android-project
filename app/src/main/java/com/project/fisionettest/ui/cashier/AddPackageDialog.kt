package com.project.fisionettest.ui.cashier

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Package
import com.project.fisionettest.databinding.DialogAddPackageBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class AddPackageDialog(private val onPackageAdded: () -> Unit) : DialogFragment() {

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

        binding.btnAddTool.setOnClickListener {
            addToolInput()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSavePackage.setOnClickListener {
            savePackage()
        }
    }

    private fun addToolInput() {
        val toolView = LayoutInflater.from(requireContext()).inflate(R.layout.item_tool_input, binding.containerTools, false)
        val btnRemove = toolView.findViewById<ImageButton>(R.id.btn_remove_tool)
        btnRemove.setOnClickListener {
            binding.containerTools.removeView(toolView)
        }
        binding.containerTools.addView(toolView)
    }

    private fun savePackage() {
        val name = binding.etPackageName.text.toString()
        val priceStr = binding.etPackagePrice.text.toString()
        
        if (name.isBlank() || priceStr.isBlank()) {
            Toast.makeText(requireContext(), "Nama dan Harga harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null) {
            Toast.makeText(requireContext(), "Harga tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val tools = mutableListOf<String>()
        for (i in 0 until binding.containerTools.childCount) {
            val child = binding.containerTools.getChildAt(i)
            val etTool = child.findViewById<EditText>(R.id.et_tool_name)
            val toolName = etTool.text.toString()
            if (toolName.isNotBlank()) {
                tools.add(toolName)
            }
        }

        lifecycleScope.launch {
            try {
                val newPackage = Package(
                    name = name,
                    price = price,
                    tools = tools
                )
                SupabaseClient.client.from("packages").insert(newPackage)
                Toast.makeText(requireContext(), "Paket berhasil dibuat", Toast.LENGTH_SHORT).show()
                onPackageAdded()
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
