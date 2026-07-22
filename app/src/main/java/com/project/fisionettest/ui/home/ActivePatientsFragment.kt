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
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.databinding.FragmentActivePatientsBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class ActivePatientsFragment : Fragment() {
    private var _binding: FragmentActivePatientsBinding? = null
    private val binding get() = _binding!!
    private lateinit var patientAdapter: PatientAdapter
    private var activePatientsList = listOf<Patient>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivePatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadActivePatients()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.etSearch.addTextChangedListener {
            applyFilter()
        }
    }

    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter { patient ->
            patient.id?.let { id ->
                val bundle = Bundle().apply {
                    putInt("patientId", id)
                }
                findNavController().navigate(
                    R.id.action_active_patients_to_patient_detail,
                    bundle
                )
            }
        }
        binding.rvPatients.apply {
            adapter = patientAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadActivePatients() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            binding.rvPatients.visibility = View.GONE
            try {
                // Fetch transactions with pending payment status, and eager load patient relation
                val transactions = SupabaseClient.client
                    .from("transactions")
                    .select(columns = Columns.raw("*, patients(*)")) {
                        filter {
                            eq("payment_status", "pending")
                        }
                    }.decodeList<Transaction>()

                // Filter out null patients and keep distinct patients
                activePatientsList = transactions
                    .mapNotNull { it.patients }
                    .distinctBy { it.id }

                applyFilter()
            } catch (e: Exception) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvPatients.visibility = View.GONE
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun applyFilter() {
        val query = binding.etSearch.text.toString().trim()
        val filteredList = if (query.isBlank()) {
            activePatientsList
        } else {
            activePatientsList.filter { patient ->
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
        loadActivePatients()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
