package com.project.fisionettest.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Appointment
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.databinding.FragmentDashboardBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadStatistics()

        binding.btnAddPatient.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_add_patient)
        }

        binding.btnViewPatients.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_patients)
        }
    }

    private fun loadStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load total patients
                val patients = SupabaseClient.client.from("patients").select().decodeList<Patient>()
                binding.tvTotalPatients.text = patients.size.toString()

                // Load today's appointments
                val appointments = SupabaseClient.client.from("appointments").select().decodeList<Appointment>()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                val todayAppointments = appointments.count { it.date == today }
                binding.tvTodayAppointments.text = todayAppointments.toString()
            } catch (e: Exception) {
                // Handle error silently or show error message
                binding.tvTotalPatients.text = "0"
                binding.tvTodayAppointments.text = "0"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
