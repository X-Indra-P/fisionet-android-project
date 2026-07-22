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
        loadAllTherapists()
    }

    private fun setupRecyclerView() {
        adapter = ApprovedTherapistAdapter(
            emptyList(),
            onTransferBranch = { profile -> showTransferBranchDialog(profile) },
            onToggleStatus   = { profile -> showToggleStatusDialog(profile) }
        )
        binding.rvApprovedTherapists.layoutManager = LinearLayoutManager(context)
        binding.rvApprovedTherapists.adapter = adapter
    }

    /** Load semua terapis: verified + inactive + suspended */
    private fun loadAllTherapists() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val list = adminRepository.getAllTherapists()
                if (list.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvApprovedTherapists.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvApprovedTherapists.visibility = View.VISIBLE
                    adapter.updateList(list)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    // ── Dialog Pindah Cabang ─────────────────────────────────────────────────
    private fun showTransferBranchDialog(profile: Profile) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_approve_therapist, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerClinic)

        val clinics = arrayOf("Cabang 1", "Cabang 2")
        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            clinics
        )
        val currentBranchName = com.project.fisionettest.utils.ClinicMapper.toName(profile.id_cabang)
        val currentIndex = clinics.indexOf(currentBranchName)
        if (currentIndex >= 0) spinner.setSelection(currentIndex)

        AlertDialog.Builder(requireContext())
            .setTitle("Pindah Cabang")
            .setMessage("Pindahkan ${profile.displayName ?: "terapis"} ke cabang mana?")
            .setView(dialogView)
            .setPositiveButton("Pindahkan") { _, _ ->
                val selected = spinner.selectedItem.toString()
                if (selected == currentBranchName) {
                    Toast.makeText(requireContext(), "Terapis sudah berada di cabang ini", Toast.LENGTH_SHORT).show()
                } else {
                    transferBranch(profile.id, selected)
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
                Toast.makeText(context, "✓ Terapis dipindahkan ke $newClinic", Toast.LENGTH_SHORT).show()
                loadAllTherapists()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memindahkan cabang: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    // ── Dialog Nonaktifkan / Aktifkan ────────────────────────────────────────
    private fun showToggleStatusDialog(profile: Profile) {
        val isActive   = profile.status == "verified"
        val name       = profile.displayName ?: "terapis ini"
        val action     = if (isActive) "nonaktifkan" else "aktifkan kembali"
        val actionBtn  = if (isActive) "Nonaktifkan" else "Aktifkan"

        AlertDialog.Builder(requireContext())
            .setTitle(if (isActive) "Nonaktifkan Terapis" else "Aktifkan Kembali")
            .setMessage("Apakah Anda yakin ingin $action akun $name?")
            .setPositiveButton(actionBtn) { _, _ ->
                toggleTherapistStatus(profile.id, isActive)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun toggleTherapistStatus(userId: String, isCurrentlyActive: Boolean) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                if (isCurrentlyActive) {
                    adminRepository.deactivateTherapist(userId)
                    Toast.makeText(context, "✓ Akun terapis dinonaktifkan", Toast.LENGTH_SHORT).show()
                } else {
                    adminRepository.reactivateTherapist(userId)
                    Toast.makeText(context, "✓ Akun terapis diaktifkan kembali", Toast.LENGTH_SHORT).show()
                }
                loadAllTherapists()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal mengubah status: ${e.message}", Toast.LENGTH_SHORT).show()
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
