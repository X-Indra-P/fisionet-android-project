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
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.data.model.Diagnosis
import com.project.fisionettest.data.model.PatientProgress
import com.project.fisionettest.databinding.FragmentDashboardBinding
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.project.fisionettest.data.model.Clinic
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: AppPreferences
    private lateinit var appointmentAdapter: com.project.fisionettest.ui.appointment.AppointmentAdapter

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
        prefs = AppPreferences(requireContext())

        setupHeader()
        setupAppointmentsRecyclerView()
        loadStatistics()

        // Tampilkan dialog jika user adalah terapis dan belum memilih cabang di sesi ini
        if (prefs.userRole == 2 && !com.project.fisionettest.MainActivity.hasSelectedBranchThisSession) {
            showBranchSelectionDialog()
        }

        binding.btnLayaniPasien.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_patients)
        }

        binding.btnPasienDilayani.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_active_patients)
        }

        binding.btnCashierMode.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_cashier)
        }

        binding.btnReportMode.visibility = View.GONE
        binding.btnReportMode.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("isAdmin", false)
            }
            findNavController().navigate(R.id.action_dashboard_to_report, bundle)
        }

        binding.btnRefreshAppointments?.setOnClickListener {
            loadTodayAppointments()
            android.widget.Toast.makeText(requireContext(), "Jadwal diperbarui", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHeader() {
        // Ambil nama dari prefs dulu (cepat), fallback ke auth metadata
        val savedName = prefs.userName
        if (!savedName.isNullOrBlank()) {
            binding.tvTitle.text = savedName
        } else {
            val user = SupabaseClient.client.auth.currentUserOrNull()
            if (user != null) {
                val metadata = user.userMetadata
                var displayName = user.email
                    ?.substringBefore("@")
                    ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    ?: "User"
                if (metadata != null && metadata.containsKey("display_name")) {
                    displayName = metadata["display_name"].toString().replace("\"", "")
                }
                binding.tvTitle.text = displayName
            } else {
                binding.tvTitle.text = "Tamu"
            }
        }

        // Tampilkan nama cabang jika ada
        val clinic = prefs.clinic
        if (!clinic.isNullOrBlank()) {
            binding.tvClinicName.text = clinic
            binding.tvClinicName.visibility = View.VISIBLE
        } else {
            binding.tvClinicName.visibility = View.GONE
        }
    }

    private fun isDateInCurrentWeek(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return false
            
            val cal = Calendar.getInstance()
            val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
            val currentYear = cal.get(Calendar.YEAR)
            
            cal.time = date
            cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(Calendar.YEAR) == currentYear
        } catch (e: Exception) {
            false
        }
    }

    private fun loadStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val therapistId = prefs.userId ?: ""
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                val currentMonth = today.substring(0, 7) // "yyyy-MM"

                // ── Ambil diagnosis yang diinput oleh terapis yang login ──
                val diagnoses = try {
                    SupabaseClient.client.from("diagnosis").select {
                        filter { eq("profile_id", therapistId) }
                    }.decodeList<Diagnosis>()
                } catch (e: Exception) {
                    emptyList()
                }

                // ── Ambil catatan perkembangan yang diinput oleh terapis yang login ──
                val progressList = try {
                    SupabaseClient.client.from("patient_progress").select {
                        filter { eq("profile_id", therapistId) }
                    }.decodeList<PatientProgress>()
                } catch (e: Exception) {
                    emptyList()
                }

                val bindingSafe = _binding ?: return@launch

                // 1. Layanan Hari Ini
                val todayDiagnoses = diagnoses.count { it.date == today && it.status.equals("Selesai", ignoreCase = true) }
                val todayProgress = progressList.count { it.date == today && it.status.equals("Selesai", ignoreCase = true) }
                val todayTotal = todayDiagnoses + todayProgress
                bindingSafe.tvTodayServices.text = todayTotal.toString()

                // 2. Layanan Minggu Ini
                val weekDiagnoses = diagnoses.count { isDateInCurrentWeek(it.date) && it.status.equals("Selesai", ignoreCase = true) }
                val weekProgress = progressList.count { isDateInCurrentWeek(it.date) && it.status.equals("Selesai", ignoreCase = true) }
                val weekTotal = weekDiagnoses + weekProgress
                bindingSafe.tvWeekServices.text = weekTotal.toString()

                // 3. Layanan Bulan Ini
                val monthDiagnoses = diagnoses.count { it.date.startsWith(currentMonth) && it.status.equals("Selesai", ignoreCase = true) }
                val monthProgress = progressList.count { it.date.startsWith(currentMonth) && it.status.equals("Selesai", ignoreCase = true) }
                val monthTotal = monthDiagnoses + monthProgress
                bindingSafe.tvMonthServices.text = monthTotal.toString()

            } catch (e: java.util.concurrent.CancellationException) {
                // Coroutine cancelled, ignore
            } catch (e: Exception) {
                _binding?.let { b ->
                    b.tvTodayServices.text = "0"
                    b.tvWeekServices.text = "0"
                    b.tvMonthServices.text = "0"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
        loadTodayAppointments()
    }

    private fun showBranchSelectionDialog() {
        lifecycleScope.launch {
            try {
                // Fetch clinics dynamically from Supabase
                val clinicList = withContext(Dispatchers.IO) {
                    SupabaseClient.client.from("cabang").select().decodeList<Clinic>()
                }
                
                if (clinicList.isEmpty()) {
                    return@launch
                }
                com.project.fisionettest.utils.ClinicMapper.updateCache(clinicList)

                val clinicNames = clinicList.map { it.nama_cabang }.toTypedArray()
                var selectedClinic = prefs.clinic ?: clinicNames.firstOrNull() ?: ""
                
                var currentIndex = clinicNames.indexOf(selectedClinic)
                if (currentIndex == -1) {
                    currentIndex = 0
                    selectedClinic = clinicNames.first()
                }

                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Cabang Bertugas")
                    .setSingleChoiceItems(clinicNames, currentIndex) { _, which ->
                        selectedClinic = clinicNames[which]
                    }
                    .setCancelable(false)
                    .setPositiveButton("Konfirmasi") { dialog, _ ->
                        val matchedClinic = clinicList.find { it.nama_cabang == selectedClinic }
                        prefs.clinic = selectedClinic
                        prefs.clinicId = matchedClinic?.id ?: 0
                        com.project.fisionettest.MainActivity.hasSelectedBranchThisSession = true
                        binding.tvClinicName.text = selectedClinic
                        binding.tvClinicName.visibility = View.VISIBLE
                        dialog.dismiss()
                        loadStatistics()
                        loadTodayAppointments()
                    }
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal memuat daftar cabang: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAppointmentsRecyclerView() {
        appointmentAdapter = com.project.fisionettest.ui.appointment.AppointmentAdapter()
        binding.rvTodayAppointments?.apply {
            adapter = appointmentAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        }
        
        appointmentAdapter.onItemClick = {
            (activity as? com.project.fisionettest.MainActivity)?.findViewById<com.google.android.material.navigation.NavigationBarView>(R.id.bottom_navigation)?.selectedItemId = R.id.appointmentFragment
        }
        
        appointmentAdapter.onServeClick = { appointment ->
            val pId = appointment.patient_id ?: 0
            if (pId > 0) {
                prefs.activeAppointmentId = appointment.id ?: -1
                val bundle = Bundle().apply {
                    putInt("patientId", pId)
                }
                findNavController().navigate(R.id.patientDetailFragment, bundle)
            } else {
                android.widget.Toast.makeText(requireContext(), "ID Pasien tidak valid", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTodayAppointments() {
        viewLifecycleOwner.lifecycleScope.launch {
            val initialBinding = _binding ?: return@launch
            initialBinding.pbAppointmentsLoading?.visibility = View.VISIBLE
            initialBinding.tvEmptyAppointments?.visibility = View.GONE
            initialBinding.rvTodayAppointments?.visibility = View.GONE
            try {
                SupabaseClient.autoMarkPastAppointmentsAsMissed()
                val clinic = prefs.clinic
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())

                val todayAppointments = SupabaseClient.client.from("appointments").select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, patients(*), profiles(*)")) {
                    filter {
                        eq("date", today)
                    }
                    order("time", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }.decodeList<Appointment>()

                val activeAppointments = todayAppointments.filter { appt ->
                    appt.status == "Terjadwal"
                }

                android.util.Log.d("FisioNetDashboard", "Today's date: $today, Clinic: $clinic, Raw Count: ${todayAppointments.size}, Active Count: ${activeAppointments.size}")
                activeAppointments.forEach { 
                    android.util.Log.d("FisioNetDashboard", "Active Appointment ID: ${it.id}, Status: ${it.status}")
                }

                val uiBinding = _binding ?: return@launch
                if (activeAppointments.isEmpty()) {
                    uiBinding.tvEmptyAppointments?.visibility = View.VISIBLE
                    uiBinding.rvTodayAppointments?.visibility = View.GONE
                } else {
                    uiBinding.tvEmptyAppointments?.visibility = View.GONE
                    uiBinding.rvTodayAppointments?.visibility = View.VISIBLE
                    appointmentAdapter.submitList(activeAppointments)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val uiBinding = _binding ?: return@launch
                uiBinding.tvEmptyAppointments?.text = "Gagal memuat janji temu"
                uiBinding.tvEmptyAppointments?.visibility = View.VISIBLE
            } finally {
                _binding?.pbAppointmentsLoading?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
