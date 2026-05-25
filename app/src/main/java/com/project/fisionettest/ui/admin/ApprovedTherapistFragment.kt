package com.project.fisionettest.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.fisionettest.R
import com.project.fisionettest.data.model.Profile
import com.project.fisionettest.data.repository.AdminRepository
import com.project.fisionettest.databinding.FragmentApprovedTherapistBinding
import kotlinx.coroutines.launch

class ApprovedTherapistFragment : Fragment() {

    private var _binding: FragmentApprovedTherapistBinding? = null
    private val binding get() = _binding!!
    private val adminRepository = AdminRepository()
    private lateinit var adapter: ApprovedTherapistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApprovedTherapistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadApprovedTherapists()
    }

    private fun setupRecyclerView() {
        adapter = ApprovedTherapistAdapter(
            emptyList(),
            onTransferBranch = { profile ->
                showTransferBranchDialog(profile)
            }
        )
        binding.rvApprovedTherapists.layoutManager = LinearLayoutManager(context)
        binding.rvApprovedTherapists.adapter = adapter
    }

    private fun loadApprovedTherapists() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val approvedList = adminRepository.getApprovedTherapists()
                if (approvedList.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvApprovedTherapists.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvApprovedTherapists.visibility = View.VISIBLE
                    adapter.updateList(approvedList)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showTransferBranchDialog(profile: Profile) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_approve_therapist, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerClinic)

        val clinics = arrayOf("Cabang 1", "Cabang 2")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, clinics)
        spinner.adapter = spinnerAdapter

        // Pre-select current clinic
        val currentIndex = clinics.indexOf(profile.clinic)
        if (currentIndex >= 0) spinner.setSelection(currentIndex)

        AlertDialog.Builder(requireContext())
            .setTitle("Pindah Cabang")
            .setMessage("Pindahkan ${profile.displayName ?: "terapis"} ke cabang mana?")
            .setView(dialogView)
            .setPositiveButton("Pindahkan") { _, _ ->
                val selectedClinic = spinner.selectedItem.toString()
                if (selectedClinic == profile.clinic) {
                    Toast.makeText(requireContext(), "Terapis sudah berada di cabang ini", Toast.LENGTH_SHORT).show()
                } else {
                    transferBranch(profile.id, selectedClinic)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun transferBranch(userId: String, newClinic: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                adminRepository.transferBranch(userId, newClinic)
                Toast.makeText(context, "Terapis berhasil dipindahkan ke $newClinic", Toast.LENGTH_SHORT).show()
                loadApprovedTherapists()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memindahkan cabang: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
