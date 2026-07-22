package com.project.fisionettest.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.databinding.FragmentHomeBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var patientAdapter: PatientAdapter
    private var allPatients = listOf<Patient>()
    private var currentSort = "A-Z"
    private var currentGenderFilter = "Semua"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadPatients()

        binding.fabAddPatient.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_add_patient)
        }

        binding.etPatientSearch.addTextChangedListener {
            applyFiltersAndSort()
        }

        binding.chipSort.setOnClickListener {
            showSortPopupMenu(it)
        }

        binding.chipFilterGender.setOnClickListener {
            showGenderFilterPopupMenu(it)
        }
    }

    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter { patient ->
            patient.id?.let { id ->
                val bundle = Bundle().apply {
                    putInt("patientId", id)
                }
                findNavController().navigate(R.id.action_home_to_patient_detail, bundle)
            }
        }
        binding.rvPatients.apply {
            adapter = patientAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadPatients() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            binding.rvPatients.visibility = View.GONE
            try {
                val patients = SupabaseClient.client.from("patients").select().decodeList<Patient>()
                allPatients = patients
                applyFiltersAndSort()
            } catch (e: Exception) {
                // Handle error
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvPatients.visibility = View.GONE
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun applyFiltersAndSort() {
        // Sync UI chip texts with state variables
        binding.chipSort.text = "Urutkan: $currentSort"
        binding.chipFilterGender.text = "Gender: $currentGenderFilter"

        val query = binding.etPatientSearch.text.toString().trim()
        
        // 1. Filter by Search Query
        var list = if (query.isBlank()) {
            allPatients
        } else {
            allPatients.filter { patient ->
                patient.name.contains(query, ignoreCase = true) ||
                patient.pekerjaan?.contains(query, ignoreCase = true) == true ||
                patient.phone?.contains(query, ignoreCase = true) == true
            }
        }

        // 2. Filter by Gender
        list = when (currentGenderFilter) {
            "Laki-laki" -> list.filter { it.gender == "L" }
            "Perempuan" -> list.filter { it.gender == "P" }
            else -> list
        }

        // 3. Sort
        list = when (currentSort) {
            "A-Z" -> list.sortedBy { it.name.lowercase(java.util.Locale.getDefault()) }
            "Z-A" -> list.sortedByDescending { it.name.lowercase(java.util.Locale.getDefault()) }
            else -> list.sortedBy { it.name.lowercase(java.util.Locale.getDefault()) }
        }

        patientAdapter.submitList(list) {
            binding.rvPatients.scrollToPosition(0)
        }
        
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvPatients.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showSortPopupMenu(view: View) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "Nama A-Z")
        popup.menu.add(0, 2, 0, "Nama Z-A")
        
        popup.setOnMenuItemClickListener { item ->
            currentSort = when (item.itemId) {
                1 -> "A-Z"
                2 -> "Z-A"
                else -> "A-Z"
            }
            binding.chipSort.text = "Urutkan: $currentSort"
            applyFiltersAndSort()
            true
        }
        popup.show()
    }

    private fun showGenderFilterPopupMenu(view: View) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "Semua")
        popup.menu.add(0, 2, 0, "Laki-laki")
        popup.menu.add(0, 3, 0, "Perempuan")
        
        popup.setOnMenuItemClickListener { item ->
            currentGenderFilter = when (item.itemId) {
                1 -> "Semua"
                2 -> "Laki-laki"
                3 -> "Perempuan"
                else -> "Semua"
            }
            binding.chipFilterGender.text = "Gender: $currentGenderFilter"
            applyFiltersAndSort()
            true
        }
        popup.show()
    }

    override fun onResume() {
        super.onResume()
        loadPatients() // Refresh data when returning to this fragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
