package com.project.fisionettest.ui.cashier

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
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.data.repository.XenditRepository
import com.project.fisionettest.databinding.FragmentTransactionDetailBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.util.Locale

class TransactionDetailFragment : Fragment() {

    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!
    private var transactionId: Int? = null
    private var currentTransaction: Transaction? = null
    private val xenditRepository = XenditRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionId = arguments?.getInt("transactionId")

        if (transactionId != null) {
            loadTransactionDetails(transactionId!!)
        } else {
            Toast.makeText(requireContext(), "Invalid Transaction ID", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Tombol Cek Status — panggil Xendit API untuk verifikasi pembayaran
        binding.btnCheckPayment.setOnClickListener {
            checkPaymentStatusFromXendit()
        }
    }

    private fun loadTransactionDetails(id: Int) {
        lifecycleScope.launch {
            try {
                val transaction = SupabaseClient.client
                    .from("transactions")
                    .select(columns = Columns.raw("*, patients(*), cabang_package(*, packages(*)), diagnosis(*), profiles(*)")) {
                        filter { eq("id", id) }
                    }
                    .decodeSingleOrNull<Transaction>()

                if (transaction != null) {
                    currentTransaction = transaction
                    displayDetails(transaction)
                } else {
                    Toast.makeText(requireContext(), "Transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat detail: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayDetails(transaction: Transaction) {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        binding.tvDate.text          = transaction.date
        binding.tvPatientName.text   = transaction.patients?.name ?: "Pasien tidak ditemukan"
        binding.tvDiagnosis.text     = transaction.diagnosis?.diagnosa ?: "-"
        binding.tvTherapist.text     = transaction.profiles?.displayName ?: "-"
        binding.tvTotalAmount.text   = format.format(transaction.total_amount)

        // Paket detail
        val pkg = transaction.cabang_package?.packages
        binding.layoutPackageDetails.removeAllViews()
        if (pkg != null) {
            val textView = android.widget.TextView(requireContext()).apply {
                text = "${pkg.name}\nAlat: Sedang memuat..."
                setTextColor(resources.getColor(R.color.black, null))
                textSize = 14f
                setPadding(0, 0, 0, 16)
            }
            binding.layoutPackageDetails.addView(textView)

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
                    val packages = SupabaseClient.getCabangPackagesForClinic(prefs.clinicId)
                    val matchedPkg = packages.firstOrNull { it.id == transaction.cabang_package_id }
                    val toolsStr = if (matchedPkg != null && matchedPkg.packages?.tools?.isNotEmpty() == true) {
                        matchedPkg.packages.tools.joinToString(", ")
                    } else if (pkg.tools.isNotEmpty()) {
                        pkg.tools.joinToString(", ")
                    } else {
                        "Tidak ada alat"
                    }
                    textView.text = "${pkg.name}\nAlat: $toolsStr"
                } catch (e: Exception) {
                    e.printStackTrace()
                    textView.text = "${pkg.name}\nAlat: ${pkg.tools.joinToString(", ")}"
                }
            }
        }

        // ── Status Pembayaran ─────────────────────────────────────────────
        updatePaymentStatusUI(transaction.payment_status ?: "pending")

        // Info cabang
        if (transaction.id_cabang != null) {
            binding.tvCabangDetail.text = com.project.fisionettest.utils.ClinicMapper.toName(transaction.id_cabang)
            binding.tvCabangDetail.visibility = View.VISIBLE
        } else {
            binding.tvCabangDetail.visibility = View.GONE
        }

        // Tombol Cek Status & Bayar hanya muncul jika:
        // - status masih pending
        val isPending  = transaction.payment_status?.lowercase() == "pending"
        val hasXendit  = !transaction.xendit_id.isNullOrBlank()
        
        binding.btnCheckPayment.visibility =
            if (isPending && hasXendit) View.VISIBLE else View.GONE

        // Tombol Bayar Sekarang
        if (isPending) {
            binding.btnPayNow.visibility = View.VISIBLE
            binding.btnPayNow.setOnClickListener {
                if (hasXendit) {
                    val invoiceUrl = "https://checkout.xendit.co/web/invoices/${transaction.xendit_id}"
                    val bundle = Bundle().apply {
                        putString("paymentUrl", invoiceUrl)
                        putInt("transactionId", transaction.id ?: -1)
                    }
                    findNavController().navigate(R.id.action_transactionDetail_to_payment, bundle)
                } else {
                    // Buat invoice baru jika belum ada xendit_id
                    generateInvoiceAndNavigate(transaction)
                }
            }
        } else {
            binding.btnPayNow.visibility = View.GONE
        }
    }

    /**
     * Generate invoice Xendit baru lalu arahkan ke PaymentFragment
     */
    private fun generateInvoiceAndNavigate(transaction: Transaction) {
        binding.btnPayNow.isEnabled = false
        binding.btnPayNow.text = "Memuat..."
        lifecycleScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                val description = "Pembayaran ${transaction.cabang_package?.packages?.name ?: "Layanan"} - ${transaction.patients?.name ?: "Pasien"}"
                val payerEmail = user?.email ?: "patient@klikfisio.com"

                val result = xenditRepository.createInvoice(
                    transactionId = transaction.id ?: -1,
                    amount = transaction.total_amount,
                    payerEmail = payerEmail,
                    description = description
                )

                // Simpan xendit_id ke transaksi
                SupabaseClient.client.from("transactions").update(
                    buildJsonObject { put("xendit_id", result.invoiceId) }
                ) { filter { eq("id", transaction.id ?: -1) } }

                val bundle = Bundle().apply {
                    putString("paymentUrl", result.invoiceUrl)
                    putInt("transactionId", transaction.id ?: -1)
                }
                findNavController().navigate(R.id.action_transactionDetail_to_payment, bundle)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal membuat invoice: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnPayNow.isEnabled = true
                binding.btnPayNow.text = "Bayar"
            }
        }
    }

    /**
     * Update tampilan badge status berdasarkan nilai payment_status.
     */
    private fun updatePaymentStatusUI(status: String) {
        val (label, color) = when (status.lowercase()) {
            "success", "paid", "settled" -> Pair("Lunas", 0xFF2E7D32.toInt())
            "failed", "expired"          -> Pair("Gagal / Kedaluwarsa", 0xFFB71C1C.toInt())
            else                         -> Pair("Menunggu Pembayaran", 0xFFF57C00.toInt())
        }
        binding.tvPaymentStatus.text      = label
        binding.tvPaymentStatus.setTextColor(color)

        // Sembunyikan tombol Cek Status & Bayar jika sudah lunas
        if (status.lowercase() in listOf("success", "paid", "settled", "failed", "expired")) {
            binding.btnCheckPayment.visibility = View.GONE
            binding.btnPayNow.visibility = View.GONE
        }
    }

    /**
     * Panggil Xendit API untuk cek status invoice terkini.
     * Jika status berubah jadi PAID/SETTLED, update DB dan UI.
     */
    private fun checkPaymentStatusFromXendit() {
        val trx = currentTransaction ?: return
        val xenditId = trx.xendit_id ?: run {
            Toast.makeText(requireContext(), "Tidak ada data invoice Xendit", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnCheckPayment.isEnabled = false
        binding.btnCheckPayment.text = "Memeriksa..."

        lifecycleScope.launch {
            try {
                // Panggil Xendit API
                val xenditStatus = xenditRepository.getInvoiceStatus(xenditId)

                val newLocalStatus = when (xenditStatus.uppercase()) {
                    "PAID", "SETTLED" -> "success"
                    "EXPIRED"         -> "expired"
                    else              -> "pending"
                }

                if (newLocalStatus != "pending") {
                    // Update status di Supabase
                    SupabaseClient.client.from("transactions").update(
                        buildJsonObject { put("payment_status", newLocalStatus) }
                    ) {
                        filter { eq("id", trx.id!!) }
                    }

                    if (newLocalStatus == "success") {
                        SupabaseClient.checkAndUpdateActiveAppointment(requireContext())
                        val diagId = trx.diagnosis_id
                        if (diagId != null) {
                            SupabaseClient.client.from("diagnosis").update(
                                buildJsonObject {
                                    put("status", "Selesai")
                                }
                            ) {
                                filter { eq("id", diagId) }
                            }
                        }
                    }

                    val msg = if (newLocalStatus == "success")
                        "✓ Pembayaran dikonfirmasi sebagai LUNAS!"
                    else
                        "✗ Invoice sudah kedaluwarsa."

                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    updatePaymentStatusUI(newLocalStatus)
                    binding.btnCheckPayment.visibility = View.GONE
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Pembayaran belum dikonfirmasi. Status: $xenditStatus",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnCheckPayment.isEnabled = true
                    binding.btnCheckPayment.text = "Cek Status"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Gagal cek status: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.btnCheckPayment.isEnabled = true
                binding.btnCheckPayment.text = "Cek Status"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
