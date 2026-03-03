package com.project.fisionettest.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
        adapter = ApprovedTherapistAdapter(emptyList())
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

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
