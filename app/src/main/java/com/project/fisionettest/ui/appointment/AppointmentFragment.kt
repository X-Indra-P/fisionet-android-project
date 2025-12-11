package com.project.fisionettest.ui.appointment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Appointment
import com.project.fisionettest.databinding.FragmentAppointmentBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class AppointmentFragment : Fragment() {
    private var _binding: FragmentAppointmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var appointmentAdapter: AppointmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadAppointments()

        binding.fabAddAppointment.setOnClickListener {
            showAddAppointmentDialog()
        }
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter()
        binding.rvAppointments.apply {
            adapter = appointmentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadAppointments() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val appointments = SupabaseClient.client.from("appointments").select().decodeList<Appointment>()
                appointmentAdapter.submitList(appointments)

                binding.tvEmpty.visibility = if (appointments.isEmpty()) View.VISIBLE else View.GONE
                binding.rvAppointments.visibility = if (appointments.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvAppointments.visibility = View.GONE
            }
        }
    }

    private fun showAddAppointmentDialog() {
        // For simplicity, showing a simple dialog
        // In a real app, you would create a proper dialog fragment with the dialog_add_appointment layout
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tambah Appointment")
            .setMessage("Fitur tambah appointment akan segera hadir")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
