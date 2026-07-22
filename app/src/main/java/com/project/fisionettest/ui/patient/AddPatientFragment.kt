package com.project.fisionettest.ui.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.databinding.FragmentAddPatientBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.project.fisionettest.utils.AppPreferences

class AddPatientFragment : Fragment() {
    private var _binding: FragmentAddPatientBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPatientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGenderDropdown()

        binding.btnSave.setOnClickListener {
            savePatient()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Laki-laki", "Perempuan")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        binding.etGender.setAdapter(adapter)
    }

    private fun savePatient() {
        val name = binding.etName.text.toString()
        val ageString = binding.etAge.text.toString()
        val gender = when(binding.etGender.text.toString()) {
            "Laki-laki" -> "L"
            "Perempuan" -> "P"
            else -> null
        }
        val phone = binding.etPhone.text.toString()
        val address = binding.etAddress.text.toString()
        val occupation = binding.etOccupation.text.toString()

        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Nama harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (gender == null) {
            Toast.makeText(requireContext(), "Jenis kelamin harus dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentSessionOrNull()?.user
                if (user == null) {
                    Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Use JsonObject to avoid sending null 'id' and 'created_at' which fails on insert
                val patientData = kotlinx.serialization.json.buildJsonObject {
                    put("name", name)
                    put("umur", ageString.toIntOrNull())
                    put("pekerjaan", occupation)
                    put("phone", phone.ifBlank { null })
                    put("address", address.ifBlank { null })
                    put("gender", binding.etGender.text.toString().substring(0, 1)) // "Laki-laki" -> "L", "Perempuan" -> "P"
                    put("profile_id", com.project.fisionettest.data.SupabaseClient.client.auth.currentUserOrNull()?.id)
                }

                 val inserted = SupabaseClient.client.from("patients")
                    .insert(patientData) { select() }
                    .decodeSingle<Patient>()

                  // Show success dialog
                  val successDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                     .setView(R.layout.dialog_payment_status)
                     .setCancelable(false)
                     .create()
                  successDialog.show()
                  successDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                  val pbLoading = successDialog.findViewById<android.widget.ProgressBar>(R.id.pb_loading)
                  val ivStatusIcon = successDialog.findViewById<android.widget.ImageView>(R.id.iv_status_icon)
                  val tvStatusTitle = successDialog.findViewById<android.widget.TextView>(R.id.tv_status_title)
                  val tvStatusMessage = successDialog.findViewById<android.widget.TextView>(R.id.tv_status_message)

                  pbLoading?.visibility = View.GONE
                  ivStatusIcon?.visibility = View.VISIBLE
                  ivStatusIcon?.setImageResource(android.R.drawable.checkbox_on_background)
                  ivStatusIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
                      androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_present)
                  )
                  tvStatusTitle?.text = "Pasien Berhasil Ditambahkan"
                  tvStatusMessage?.text = "Mengalihkan ke pengisian diagnosis pasien..."

                  // Delay for 3 seconds
                  kotlinx.coroutines.delay(3000)
                  successDialog.dismiss()

                  val bundle = Bundle().apply {
                      putInt("patientId", inserted.id ?: 0)
                  }

                  findNavController().navigate(
                      R.id.action_addPatientFragment_to_addDiagnosisFragment,
                      bundle,
                      androidx.navigation.navOptions {
                          popUpTo(R.id.addPatientFragment) {
                              inclusive = true
                          }
                      }
                  )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
