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
import com.project.fisionettest.data.model.MedicalRecord
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.databinding.FragmentDashboardBinding
import io.github.jan.supabase.gotrue.auth
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

        // Get logged-in user
        val user = SupabaseClient.client.auth.currentUserOrNull()
        if (user != null) {
            // Try to get name from metadata
            val metadata = user.userMetadata
            var displayName = user.email?.substringBefore("@")?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "User"

            if (metadata != null && metadata.containsKey("display_name")) {
                // Remove quotes which might be present if it's a JsonElement string
                displayName = metadata["display_name"].toString().replace("\"", "")
            }
            
            binding.tvTitle.text = displayName
        } else {
             binding.tvTitle.text = "Tamu"
        }

        loadStatistics()

        binding.btnAddPatient.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_add_patient)
        }

        binding.btnViewPatientsCard.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_patients)
        }
    }

    private fun loadStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load total patients
                val patients = SupabaseClient.client.from("patients").select().decodeList<Patient>()
                binding.tvTotalPatients.text = patients.size.toString()

                // Load medical records for statistics
                val records = SupabaseClient.client.from("medical_records").select().decodeList<MedicalRecord>()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                val currentMonth = today.substring(0, 7) // "yyyy-MM"

                // Calculate today's patients (unique patients with records today)
                val todayPatients = records.filter { it.date == today }
                    .distinctBy { it.patient_id }
                    .count()
                binding.tvTodayPatients.text = todayPatients.toString()

                // Calculate month's patients (unique patients with records this month)
                val monthPatients = records.filter { it.date.startsWith(currentMonth) }
                    .distinctBy { it.patient_id }
                    .count()
                binding.tvMonthPatients.text = monthPatients.toString()

            } catch (e: Exception) {
                // Handle error silently or show error message
                binding.tvTotalPatients.text = "0"
                binding.tvTodayPatients.text = "0"
                binding.tvMonthPatients.text = "0"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
