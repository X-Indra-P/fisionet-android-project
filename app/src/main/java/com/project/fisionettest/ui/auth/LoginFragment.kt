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
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val adminRepository = AdminRepository()
    private lateinit var prefs: AppPreferences

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
        prefs = AppPreferences(requireContext())

        // Attempt to load session from storage
        viewLifecycleOwner.lifecycleScope.launch {
            var sessionLoaded = false
            try {
                SupabaseClient.client.auth.loadFromStorage()
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                if (session != null) {
                    // Proactively refresh to get a new JWT
                    SupabaseClient.client.auth.refreshCurrentSession()
                    sessionLoaded = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Session expired/invalid, clear local prefs
                prefs.clearSession()
                try { SupabaseClient.client.auth.signOut() } catch(authEx: Exception) {}
            }

            // Check if user is already logged in with a verified session
            if (sessionLoaded && SupabaseClient.client.auth.currentSessionOrNull() != null) {
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
            } else if (password.length < 8) {
                binding.tilPassword.error = "Password minimal 8 karakter"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            loginUser(email, password)
        }

        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.btnForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val inputEditText = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = "Masukkan email terdaftar"
        }
        val layout = android.widget.FrameLayout(requireContext()).apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(inputEditText)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Lupa Password")
            .setMessage("Kami akan mengirimkan instruksi atur ulang kata sandi ke alamat email Anda.")
            .setView(layout)
            .setPositiveButton("Kirim") { dialog, _ ->
                val email = inputEditText.text.toString().trim()
                if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(requireContext(), "Email tidak valid", Toast.LENGTH_SHORT).show()
                } else {
                    sendResetPasswordEmail(email)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun sendResetPasswordEmail(email: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.resetPasswordForEmail(email = email, redirectUrl = "fisionet://reset-password")
                Toast.makeText(
                    requireContext(),
                    "Email pemulihan sandi telah dikirim. Silakan periksa email Anda.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Gagal mengirim email: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
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
                com.project.fisionettest.MainActivity.shouldShowBranchSelection = true
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
                val user = SupabaseClient.client.auth.currentUserOrNull()
                val userId = user?.id
                if (userId != null) {
                    val profile = adminRepository.getUserProfile(userId)

                    if (profile != null) {
                        // ── Simpan sesi ke SharedPreferences ──────────────────
                        prefs.userId   = userId
                        prefs.userRole = profile.role
                        prefs.clinic   = com.project.fisionettest.utils.ClinicMapper.toName(profile.id_cabang)  // null jika belum di-assign

                        // Ambil display name dari metadata
                        val metadata = user.userMetadata
                        var displayName = user.email?.substringBefore("@") ?: "User"
                        if (metadata != null && metadata.containsKey("display_name")) {
                            displayName = metadata["display_name"].toString().replace("\"", "")
                        }
                        prefs.userName = displayName
                        // ──────────────────────────────────────────────────────

                        when (profile.role) {
                            1 -> { // Admin
                                findNavController().navigate(R.id.action_login_to_admin_dashboard)
                            }
                            2 -> { // Therapist
                                when (profile.status) {
                                    "verified" -> {
                                        com.project.fisionettest.MainActivity.hasSelectedBranchThisSession = false
                                        findNavController().navigate(R.id.action_login_to_dashboard)
                                    }
                                    "inactive", "suspended" -> {
                                        Toast.makeText(
                                            requireContext(),
                                            "Akun Anda telah dinonaktifkan. Hubungi admin.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        SupabaseClient.client.auth.signOut()
                                        prefs.clearSession()
                                    }
                                    "pending" -> {
                                        Toast.makeText(
                                            requireContext(),
                                            "Akun Anda sedang menunggu verifikasi admin.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        SupabaseClient.client.auth.signOut()
                                        prefs.clearSession()
                                    }
                                    "rejected" -> {
                                        Toast.makeText(
                                            requireContext(),
                                            "Akun Anda telah ditolak oleh admin.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        SupabaseClient.client.auth.signOut()
                                        prefs.clearSession()
                                    }
                                    else -> {
                                        com.project.fisionettest.MainActivity.hasSelectedBranchThisSession = false
                                        findNavController().navigate(R.id.action_login_to_dashboard)
                                    }
                                }
                            }
                            else -> {
                                findNavController().navigate(R.id.action_login_to_dashboard)
                            }
                        }
                    } else {
                        // Profile not found — navigate ke dashboard sebagai fallback
                        findNavController().navigate(R.id.action_login_to_dashboard)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat profil: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
