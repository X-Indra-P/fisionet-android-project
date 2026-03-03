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
import com.project.fisionettest.data.repository.AdminRepository
import com.project.fisionettest.databinding.FragmentPendingTherapistBinding
import kotlinx.coroutines.launch

class PendingTherapistFragment : Fragment() {

    private var _binding: FragmentPendingTherapistBinding? = null
    private val binding get() = _binding!!
    private val adminRepository = AdminRepository()
    private lateinit var adapter: PendingTherapistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendingTherapistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadPendingTherapists()
    }

    private fun setupRecyclerView() {
        adapter = PendingTherapistAdapter(
            emptyList(),
            onApprove = { profile ->
                showApproveDialog(profile)
            },
            onReject = { profile ->
                rejectTherapist(profile.id)
            }
        )
        binding.rvPendingTherapists.layoutManager = LinearLayoutManager(context)
        binding.rvPendingTherapists.adapter = adapter
    }

    private fun loadPendingTherapists() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val pendingList = adminRepository.getPendingTherapists()
                if (pendingList.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvPendingTherapists.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvPendingTherapists.visibility = View.VISIBLE
                    adapter.updateList(pendingList)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showApproveDialog(profile: com.project.fisionettest.data.model.Profile) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Verifikasi Terapis")
        builder.setMessage("Pilih cabang untuk ${profile.displayName}")

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_approve_therapist, null)
        val spinner = view.findViewById<Spinner>(R.id.spinnerClinic)
        val clinics = arrayOf("Cabang 1", "Cabang 2")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, clinics)
        spinner.adapter = adapter

        builder.setView(view)
        builder.setPositiveButton("Terima") { _, _ ->
            val selectedClinic = spinner.selectedItem.toString()
            verifyTherapist(profile.id, selectedClinic)
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun verifyTherapist(userId: String, clinic: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                adminRepository.verifyTherapist(userId, clinic)
                Toast.makeText(context, "Terapis berhasil diverifikasi di $clinic", Toast.LENGTH_SHORT).show()
                loadPendingTherapists()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memverifikasi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun rejectTherapist(userId: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                adminRepository.rejectTherapist(userId)
                Toast.makeText(context, "Terapis ditolak", Toast.LENGTH_SHORT).show()
                loadPendingTherapists()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menolak: ${e.message}", Toast.LENGTH_SHORT).show()
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
