package com.project.fisionettest.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.repository.AdminRepository
import com.project.fisionettest.databinding.FragmentLoginBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val adminRepository = AdminRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Attempt to load session from storage
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.loadFromStorage()
            } catch (e: Exception) {
                // Failed to load session, user needs to login
            }
            
            // Check if user is already logged in
            if (SupabaseClient.client.auth.currentSessionOrNull() != null) {
                checkUserRoleAndNavigate()
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            binding.tilEmail.error = null
            binding.tilPassword.error = null
            var isValid = true

            if (email.isBlank()) {
                binding.tilEmail.error = "Email harus diisi"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Format email tidak valid"
                isValid = false
            }
            
            if (password.isBlank()) {
                binding.tilPassword.error = "Password harus diisi"
                isValid = false
            } else if (password.length < 6) {
                binding.tilPassword.error = "Password minimal 6 karakter"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            loginUser(email, password)
        }

        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun loginUser(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                Toast.makeText(requireContext(), "Login berhasil", Toast.LENGTH_SHORT).show()
                checkUserRoleAndNavigate()
            } catch (e: Exception) {
                val errorMessage = e.message ?: ""
                if (errorMessage.contains("Invalid login credentials", ignoreCase = true)) {
                    binding.tilPassword.error = "Password salah"
                    Toast.makeText(requireContext(), "Password salah", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Login gagal: $errorMessage", Toast.LENGTH_LONG).show()
                }
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun checkUserRoleAndNavigate() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val profile = adminRepository.getUserProfile(userId)
                    
                    if (profile != null) {
                        when (profile.role) {
                            1 -> { // Admin
                                findNavController().navigate(R.id.action_login_to_admin_dashboard)
                            }
                            2 -> { // Therapist
                                when (profile.status) {
                                    "verified" -> {
                                        findNavController().navigate(R.id.action_login_to_dashboard)
                                    }
                                    "pending" -> {
                                        Toast.makeText(requireContext(), "Akun Anda sedang menunggu verifikasi admin.", Toast.LENGTH_LONG).show()
                                        // Optionally verify if we should sign out or let them stay logged in but on login screen
                                        SupabaseClient.client.auth.signOut() 
                                    }
                                    "rejected" -> {
                                        Toast.makeText(requireContext(), "Akun Anda telah ditolak oleh admin.", Toast.LENGTH_LONG).show()
                                        SupabaseClient.client.auth.signOut()
                                    }
                                    else -> {
                                         // Fallback
                                         findNavController().navigate(R.id.action_login_to_dashboard)
                                    }
                                }
                            }
                            else -> {
                                // Default fallback
                                findNavController().navigate(R.id.action_login_to_dashboard)
                            }
                        }
                    } else {
                        // Profile not found, maybe create it or just let them in (legacy support?)
                        // For now let's assume if no profile, treated as therapist pending/verified depending on policy
                        // Or navigate to dashboard if we want to allow legacy users
                         findNavController().navigate(R.id.action_login_to_dashboard)
                    }
                }
            } catch (e: Exception) {
                 Toast.makeText(requireContext(), "Gagal memuat profil: ${e.message}", Toast.LENGTH_SHORT).show()
                 binding.btnLogin.isEnabled = true
                 binding.progressBar.visibility = View.GONE
            } finally {
                // Determine if we should hide progress bar or keep it if navigating?
                // Navigation will destroy view anyway
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
