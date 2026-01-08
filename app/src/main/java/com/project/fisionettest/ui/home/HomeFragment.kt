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
            filterPatients(it.toString())
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
            try {
                val patients = SupabaseClient.client.from("patients").select().decodeList<Patient>()
                allPatients = patients
                patientAdapter.submitList(patients)
                
                binding.tvEmpty.visibility = if (patients.isEmpty()) View.VISIBLE else View.GONE
                binding.rvPatients.visibility = if (patients.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                // Handle error
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvPatients.visibility = View.GONE
            }
        }
    }

    private fun filterPatients(query: String) {
        val filteredList = if (query.isBlank()) {
            allPatients
        } else {
            allPatients.filter { patient ->
                patient.name.contains(query, ignoreCase = true) ||
                patient.pekerjaan?.contains(query, ignoreCase = true) == true ||
                patient.phone?.contains(query, ignoreCase = true) == true
            }
        }
        patientAdapter.submitList(filteredList)
        
        binding.tvEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        binding.rvPatients.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
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
