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
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Patient
import com.project.fisionettest.databinding.FragmentAddPatientBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

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

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentSessionOrNull()?.user
                if (user == null) {
                    Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val newPatient = Patient(
                    name = name,
                    umur = ageString.toIntOrNull(),
                    pekerjaan = occupation,
                    therapist_id = user.id,
                    phone = phone.ifBlank { null },
                    address = address.ifBlank { null },
                    gender = gender
                )

                SupabaseClient.client.from("patients").insert(newPatient)
                Toast.makeText(requireContext(), "Pasien berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
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
