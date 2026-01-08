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
import com.project.fisionettest.databinding.FragmentEditPatientBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class EditPatientFragment : Fragment() {
    private var _binding: FragmentEditPatientBinding? = null
    private val binding get() = _binding!!
    private var patientId: Int = 0
    private var currentPatient: Patient? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPatientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        patientId = arguments?.getInt("patientId") ?: 0

        setupGenderDropdown()
        loadPatientData()

        binding.btnSave.setOnClickListener {
            updatePatient()
        }
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Laki-laki", "Perempuan")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        binding.etGender.setAdapter(adapter)
    }

    private fun loadPatientData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val patient = SupabaseClient.client.from("patients").select {
                    filter { eq("id", patientId) }
                }.decodeSingle<Patient>()

                currentPatient = patient
                displayPatientData(patient)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayPatientData(patient: Patient) {
        binding.etName.setText(patient.name)
        binding.etAge.setText(patient.umur?.toString() ?: "")
        
        binding.etGender.setText(when(patient.gender) {
            "L" -> "Laki-laki"
            "P" -> "Perempuan"
            else -> ""
        }, false)
        binding.etPhone.setText(patient.phone ?: "")
        binding.etAddress.setText(patient.address ?: "")
        binding.etOccupation.setText(patient.pekerjaan ?: "")
    }

    private fun updatePatient() {
        val name = binding.etName.text.toString()
        val ageString = binding.etAge.text.toString()
        val gender = when(binding.etGender.text.toString()) {
            "Laki-laki" -> "L"
            "Perempuan" -> "P"
            else -> currentPatient?.gender
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updatedPatient = currentPatient?.copy(
                    name = name,
                    umur = ageString.toIntOrNull(),
                    pekerjaan = occupation,
                    phone = phone.ifBlank { null },
                    address = address.ifBlank { null },
                    gender = gender
                )

                SupabaseClient.client.from("patients").update(updatedPatient!!) {
                    filter { eq("id", patientId) }
                }

                Toast.makeText(requireContext(), "Pasien berhasil diupdate", Toast.LENGTH_SHORT).show()
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
