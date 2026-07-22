package com.project.fisionettest.ui.profile

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Profile
import com.project.fisionettest.databinding.FragmentTherapistProfileBinding
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TherapistProfileFragment : Fragment() {
    private var _binding: FragmentTherapistProfileBinding? = null
    private val binding get() = _binding!!

    private var isEditMode = false
    private var currentProfile: Profile? = null
    private var selectedImageUri: Uri? = null
    private var selectedImageBytes: ByteArray? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivAvatar.load(it) {
                transformations(CircleCropTransformation())
            }
            // Read bytes in IO dispatcher
            lifecycleScope.launch {
                selectedImageBytes = withContext(Dispatchers.IO) {
                    try {
                        requireContext().contentResolver.openInputStream(it)?.use { stream ->
                            stream.readBytes()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTherapistProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.headerLayout.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            if (isEditMode) {
                saveProfileChanges()
            } else {
                setEditMode(true)
            }
        }

        binding.btnEditPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.etDateOfBirth.setOnClickListener {
            if (isEditMode) {
                showDatePicker()
            }
        }

        loadProfileData()
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        binding.etName.isEnabled = enabled
        binding.etPlaceOfBirth.isEnabled = enabled
        binding.etPhone.isEnabled = enabled
        binding.etAddress.isEnabled = enabled
        binding.btnEditPhoto.visibility = if (enabled) View.VISIBLE else View.GONE
        
        binding.btnSave.text = if (enabled) "Simpan" else "Ubah Profil"
    }

    private fun loadProfileData() {
        val userId = AppPreferences(requireContext()).userId ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(context, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        lifecycleScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    SupabaseClient.client.from("profiles")
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }.decodeSingleOrNull<Profile>()
                }

                if (profile != null) {
                    currentProfile = profile
                    binding.etName.setText(profile.displayName ?: "")
                    binding.etPlaceOfBirth.setText(profile.placeOfBirth ?: "")
                    binding.etDateOfBirth.setText(profile.dateOfBirth ?: "")
                    binding.etPhone.setText(profile.phone ?: "")
                    binding.etAddress.setText(profile.address ?: "")
                    
                    updateAgeDisplay(profile.dateOfBirth)

                    if (!profile.avatarUrl.isNullOrEmpty()) {
                        binding.ivAvatar.load(profile.avatarUrl) {
                            placeholder(R.drawable.ic_launcher_foreground)
                            error(R.drawable.ic_launcher_foreground)
                            transformations(CircleCropTransformation())
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal memuat profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentDob = binding.etDateOfBirth.text.toString()
        if (currentDob.isNotEmpty()) {
            try {
                val parts = currentDob.split("-")
                calendar.set(Calendar.YEAR, parts[0].toInt())
                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            
            val myFormat = "yyyy-MM-dd"
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            val selectedDateStr = sdf.format(calendar.time)
            binding.etDateOfBirth.setText(selectedDateStr)
            updateAgeDisplay(selectedDateStr)
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateAgeDisplay(dateOfBirthStr: String?) {
        if (dateOfBirthStr.isNullOrEmpty()) {
            binding.etAge.setText("-")
            return
        }
        try {
            val parts = dateOfBirthStr.split("-")
            val birthYear = parts[0].toInt()
            val birthMonth = parts[1].toInt()
            val birthDay = parts[2].toInt()

            val today = Calendar.getInstance()
            val currentYear = today.get(Calendar.YEAR)
            val currentMonth = today.get(Calendar.MONTH) + 1
            val currentDay = today.get(Calendar.DAY_OF_MONTH)

            var age = currentYear - birthYear
            if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                age--
            }
            binding.etAge.setText("$age Tahun")
        } catch (e: Exception) {
            e.printStackTrace()
            binding.etAge.setText("-")
        }
    }

    private fun saveProfileChanges() {
        val name = binding.etName.text.toString().trim()
        val placeOfBirth = binding.etPlaceOfBirth.text.toString().trim()
        val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "Nama tidak boleh kosong"
            return
        } else {
            binding.tilName.error = null
        }

        val userId = AppPreferences(requireContext()).userId ?: ""

        lifecycleScope.launch {
            try {
                // Show loading
                binding.btnSave.isEnabled = false
                binding.btnSave.text = "Menyimpan..."

                var finalAvatarUrl = currentProfile?.avatarUrl

                // 1. Upload avatar jika ada gambar baru yang dipilih
                selectedImageBytes?.let { bytes ->
                    withContext(Dispatchers.IO) {
                        val fileName = "avatar_${userId}_${System.currentTimeMillis()}.jpg"
                        val bucket = SupabaseClient.client.storage.from("avatars")
                        bucket.upload(fileName, bytes, upsert = true)
                        finalAvatarUrl = bucket.publicUrl(fileName)
                    }
                }

                // 2. Buat profil terupdate
                val updatedProfile = Profile(
                    id = userId,
                    displayName = name,
                    role = currentProfile?.role ?: 2,
                    status = currentProfile?.status ?: "pending",
                    id_cabang = currentProfile?.id_cabang,
                    createdAt = currentProfile?.createdAt,
                    avatarUrl = finalAvatarUrl,
                    placeOfBirth = placeOfBirth.ifEmpty { null },
                    dateOfBirth = dateOfBirth.ifEmpty { null },
                    phone = phone.ifEmpty { null },
                    address = address.ifEmpty { null }
                )

                // 3. Simpan ke database Supabase
                withContext(Dispatchers.IO) {
                    SupabaseClient.client.from("profiles").update(updatedProfile) {
                        filter {
                            eq("id", userId)
                        }
                    }
                }

                Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                setEditMode(false)
                loadProfileData() // Reload terbaru
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = if (isEditMode) "Simpan" else "Ubah Profil"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
