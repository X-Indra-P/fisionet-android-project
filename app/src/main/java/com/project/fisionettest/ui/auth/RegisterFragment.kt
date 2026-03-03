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
import com.project.fisionettest.databinding.FragmentRegisterBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            binding.tilName.error = null
            binding.tilEmail.error = null
            binding.tilPassword.error = null
            var isValid = true

            if (name.isBlank()) {
                binding.tilName.error = "Nama harus diisi"
                isValid = false
            }

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

            registerUser(name, email, password)
        }

        binding.btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        binding.btnRegister.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("display_name", name)
                    }
                }

                
                Toast.makeText(requireContext(), "Registrasi berhasil! Silakan login.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_register_to_login)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Registrasi gagal: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
