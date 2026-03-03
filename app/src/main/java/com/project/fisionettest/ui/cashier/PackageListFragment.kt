package com.project.fisionettest.ui.cashier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Package
import com.project.fisionettest.databinding.FragmentPackageListBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class PackageListFragment : Fragment() {

    private var _binding: FragmentPackageListBinding? = null
    private val binding get() = _binding!!
    private lateinit var packageAdapter: PackageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackageListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadPackages()

        binding.fabAddPackage.setOnClickListener {
            showAddEditDialog(null)
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        packageAdapter = PackageAdapter()
        binding.rvPackages.apply {
            adapter = packageAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        packageAdapter.onEditClick = { pkg ->
            showAddEditDialog(pkg)
        }

        packageAdapter.onDeleteClick = { pkg ->
            confirmDelete(pkg)
        }
    }

    private fun loadPackages() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val packages = SupabaseClient.client.from("packages").select().decodeList<Package>()
                packageAdapter.submitList(packages)
                binding.tvEmpty.visibility = if (packages.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat paket: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showAddEditDialog(pkg: Package?) {
        val dialog = AddPackageDialog(pkg) {
            loadPackages() // Refresh after save
        }
        dialog.show(parentFragmentManager, "AddPackageDialog")
    }

    private fun confirmDelete(pkg: Package) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Paket")
            .setMessage("Apakah Anda yakin ingin menghapus paket '${pkg.name}'?")
            .setPositiveButton("Hapus") { _, _ ->
                deletePackage(pkg)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePackage(pkg: Package) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.from("packages").delete {
                    filter {
                        eq("id", pkg.id!!)
                    }
                }
                Toast.makeText(requireContext(), "Paket dihapus", Toast.LENGTH_SHORT).show()
                loadPackages()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
