package com.project.fisionettest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.databinding.ActivityMainBinding
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.parseSessionFromUrl
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    companion object {
        var hasSelectedBranchThisSession = false
        var shouldShowBranchSelection = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        // Handle deep link jika activity dibuka langsung dari Xendit callback
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Proactively refresh the session when app is resumed to prevent JWT expired issues
        lifecycleScope.launch {
            try {
                if (SupabaseClient.client.auth.currentSessionOrNull() != null) {
                    SupabaseClient.client.auth.refreshCurrentSession()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Dipanggil ketika app sudah berjalan di background dan user kembali dari browser
        handleIntent(intent)
    }

    /**
     * Handle deep link dari Xendit:
     *   Sukses  → fisionet://payment-success?trx_id=123
     *   Gagal   → fisionet://payment-failed?trx_id=123
     */
    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return

        when {
            data.scheme == "fisionet" && data.host == "payment-success" -> {
                // Ambil trx_id dari query param
                val trxId = data.getQueryParameter("trx_id")?.toIntOrNull()
                if (trxId != null) {
                    updatePaymentStatus(trxId, "success")
                } else {
                    // Tidak ada trx_id, tampilkan pesan dan navigate ke riwayat
                    Toast.makeText(this, "✓ Pembayaran berhasil!", Toast.LENGTH_LONG).show()
                    safeNavigateToHistory()
                }
            }
            data.scheme == "fisionet" && data.host == "payment-failed" -> {
                val trxId = data.getQueryParameter("trx_id")?.toIntOrNull()
                if (trxId != null) {
                    updatePaymentStatus(trxId, "failed")
                } else {
                    Toast.makeText(this, "✗ Pembayaran gagal atau dibatalkan.", Toast.LENGTH_LONG).show()
                    safeNavigateToHistory()
                }
            }
            data.scheme == "fisionet" && data.host == "reset-password" -> {
                handleResetPasswordDeeplink(intent)
            }
        }
    }

    private fun handleResetPasswordDeeplink(intent: Intent?) {
        val uriStr = intent?.data?.toString() ?: return
        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.parseSessionFromUrl(uriStr)
                SupabaseClient.client.auth.importSession(session)
                showResetPasswordDialog()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Gagal memproses link reset: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showResetPasswordDialog() {
        val inputEditText = com.google.android.material.textfield.TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Masukkan password baru"
        }
        val layout = android.widget.FrameLayout(this).apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(inputEditText)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Atur Ulang Password")
            .setMessage("Masukkan password baru Anda (minimal 8 karakter).")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Simpan") { dialog, _ ->
                val newPassword = inputEditText.text.toString().trim()
                if (newPassword.length < 8) {
                    Toast.makeText(this, "Password minimal 8 karakter", Toast.LENGTH_SHORT).show()
                    showResetPasswordDialog() // Show again
                } else {
                    updatePasswordAndLogout(newPassword)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun updatePasswordAndLogout(newPassword: String) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.updateUser {
                    password = newPassword
                }
                Toast.makeText(this@MainActivity, "Password berhasil diperbarui! Silakan login kembali.", Toast.LENGTH_LONG).show()
                // Logout to clear session
                SupabaseClient.client.auth.signOut()
                // Clear prefs
                val prefs = AppPreferences(this@MainActivity)
                prefs.clearSession()
                // Navigate back to login screen
                navController.navigate(R.id.loginFragment, null, androidx.navigation.navOptions {
                    popUpTo(R.id.nav_graph) {
                        inclusive = true
                    }
                })
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Gagal memperbarui password: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Update payment_status transaksi di Supabase berdasarkan ID lokal.
     * Kemudian navigate ke riwayat transaksi.
     */
    private fun updatePaymentStatus(transactionId: Int, status: String) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.from("transactions").update(
                    buildJsonObject {
                        put("payment_status", status)
                    }
                ) {
                    filter { eq("id", transactionId) }
                }

                if (status == "success") {
                    SupabaseClient.checkAndUpdateActiveAppointment(this@MainActivity)
                }

                val message = if (status == "success")
                    "✓ Pembayaran berhasil dikonfirmasi!"
                else
                    "✗ Pembayaran gagal. Transaksi dibatalkan."

                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Gagal update status: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Navigate ke riwayat transaksi apapun hasilnya
                safeNavigateToHistory()
            }
        }
    }

    private fun safeNavigateToHistory() {
        try {
            navController.navigate(R.id.transactionHistoryFragment)
        } catch (e: Exception) {
            // NavController mungkin belum siap saat deep link masuk sebelum navigate
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.dashboardFragment,
                R.id.homeFragment,
                R.id.appointmentFragment,
                R.id.profileFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }
    }
}