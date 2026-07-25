package com.project.fisionettest.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.databinding.FragmentProfileBinding
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from
import com.project.fisionettest.data.model.Profile

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_therapist_profile)
        }


        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showEditProfileDialog() {
        val context = requireContext()
        val density = resources.displayMetrics.density
        val paddingHorizontal = (24 * density).toInt()
        val paddingTop = (24 * density).toInt()

        val inputLayout = com.google.android.material.textfield.TextInputLayout(context).apply {
            hint = "Nama Tampilan Baru"
            setPadding(paddingHorizontal, paddingTop, paddingHorizontal, 0)
        }
        val input = com.google.android.material.textfield.TextInputEditText(context)
        inputLayout.addView(input)

        MaterialAlertDialogBuilder(context)
            .setTitle("Edit Profil")
            .setView(inputLayout)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotBlank()) {
                    updateProfileName(newName)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateProfileName(newName: String) {
        lifecycleScope.launch {
            try {
                // Update user metadata in Supabase
                SupabaseClient.client.auth.updateUser {
                    data = kotlinx.serialization.json.buildJsonObject {
                        put("display_name", newName)
                    }
                }
                loadUserProfile() // Refresh UI
                Toast.makeText(context, "Profil diperbarui", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                if (user != null) {
                    binding.tvEmail.text = user.email

                    val userId = AppPreferences(requireContext()).userId ?: ""
                    if (userId.isNotEmpty()) {
                        val profile = withContext(Dispatchers.IO) {
                            SupabaseClient.client.from("profiles")
                                .select {
                                    filter {
                                        eq("id", userId)
                                    }
                                }.decodeSingleOrNull<Profile>()
                        }
                        if (profile != null) {
                            binding.tvDisplayName.text = profile.displayName ?: "User"
                            if (!profile.avatarUrl.isNullOrEmpty()) {
                                binding.ivProfile.load(profile.avatarUrl) {
                                    placeholder(R.drawable.ic_launcher_foreground)
                                    error(R.drawable.ic_launcher_foreground)
                                    transformations(coil.transform.CircleCropTransformation())
                                }
                            }
                        } else {
                            val metadata = user.userMetadata
                            var displayName = "User"
                            if (metadata != null && metadata.containsKey("display_name")) {
                                displayName = metadata["display_name"].toString().replace("\"", "")
                            }
                            binding.tvDisplayName.text = displayName
                        }
                    }
                } else {
                    binding.tvEmail.text = "-"
                    binding.tvDisplayName.text = "Tamu"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
                // Hapus data sesi lokal (clinic, userId, dll)
                AppPreferences(requireContext()).clearSession()
                Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_profile_to_login)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
