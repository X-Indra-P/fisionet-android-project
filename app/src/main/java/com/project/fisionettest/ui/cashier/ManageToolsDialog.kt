package com.project.fisionettest.ui.cashier

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Tool
import com.project.fisionettest.databinding.DialogManageToolsBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class ManageToolsDialog(
    private val onToolsChanged: () -> Unit
) : DialogFragment() {

    private var _binding: DialogManageToolsBinding? = null
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
        _binding = DialogManageToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadExistingTools()

        binding.btnAddTool.setOnClickListener {
            addTool()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadExistingTools() {
        binding.containerExistingTools.removeAllViews()
        lifecycleScope.launch {
            try {
                val tools = SupabaseClient.client.from("tools").select().decodeList<Tool>().sortedBy { it.nama_tools }
                tools.forEach { tool ->
                    val toolItemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_tool_manage, binding.containerExistingTools, false)
                    val tvName = toolItemView.findViewById<TextView>(R.id.tv_tool_name)
                    val btnDelete = toolItemView.findViewById<ImageButton>(R.id.btn_delete_tool)

                    tvName.text = tool.nama_tools
                    btnDelete.setOnClickListener {
                        confirmDeleteTool(tool)
                    }

                    binding.containerExistingTools.addView(toolItemView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal memuat daftar alat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addTool() {
        val name = binding.etNewToolName.text.toString().trim()
        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Nama alat tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val existingTools = SupabaseClient.client.from("tools").select().decodeList<Tool>()
                if (existingTools.any { it.nama_tools.equals(name, ignoreCase = true) }) {
                    Toast.makeText(requireContext(), "Alat dengan nama tersebut sudah ada", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                SupabaseClient.client.from("tools").insert(Tool(nama_tools = name))
                Toast.makeText(requireContext(), "Alat berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                binding.etNewToolName.text?.clear()
                loadExistingTools()
                onToolsChanged()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal menambahkan alat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteTool(tool: Tool) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Alat")
            .setMessage("Apakah Anda yakin ingin menghapus alat '${tool.nama_tools}'? Tindakan ini juga akan menghapus alat ini dari paket terkait.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    try {
                        SupabaseClient.client.from("tools").delete {
                            filter { eq("id", tool.id!!) }
                        }
                        Toast.makeText(requireContext(), "Alat berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadExistingTools()
                        onToolsChanged()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(requireContext(), "Gagal menghapus alat: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
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
