package com.project.fisionettest.ui.cashier

import android.app.AlertDialog
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
import com.project.fisionettest.databinding.FragmentReceiptBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.util.Locale

class ReceiptFragment : Fragment() {
    private var _binding: FragmentReceiptBinding? = null
    private val binding get() = _binding!!
    private var transactionId: Int = 0
    private var currentTransaction: Transaction? = null
    private val xenditRepository = XenditRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionId = arguments?.getInt("transactionId") ?: 0

        loadReceiptData()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnPayCash.setOnClickListener {
            processCashPayment()
        }

        binding.btnPayCashless.setOnClickListener {
            processCashlessPayment()
        }
    }

    private fun loadReceiptData() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.pbLoading.visibility = View.VISIBLE
            try {
                // Fetch transaction details with patient, package, and diagnosis relationships
                val trx = SupabaseClient.client.from("transactions")
                    .select(columns = Columns.raw("*, patients(*), cabang_package(*, packages(*)), diagnosis(*, profiles(*)), profiles(*)")) {
                        filter { eq("id", transactionId) }
                    }.decodeList<Transaction>().firstOrNull()

                if (trx == null) {
                    Toast.makeText(requireContext(), "Transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
                    binding.pbLoading.visibility = View.GONE
                    findNavController().popBackStack()
                    return@launch
                }

                currentTransaction = trx
                displayReceipt(trx)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat rincian struk: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun displayReceipt(trx: Transaction) {
        binding.tvClinicBranch.text = com.project.fisionettest.utils.ClinicMapper.toName(trx.id_cabang)
        binding.tvReceiptDate.text = "Tanggal: ${trx.date}"
        binding.tvPatientName.text = trx.patients?.name ?: "-"
        binding.tvTherapistName.text = trx.profiles?.displayName ?: trx.diagnosis?.profiles?.displayName ?: "-"
        binding.tvDiagnosis.text = trx.diagnosis?.diagnosa ?: "-"
        binding.tvPackageName.text = trx.cabang_package?.packages?.name ?: "-"
        
        val toolsList = trx.cabang_package?.packages?.tools
        val toolsText = if (!toolsList.isNullOrEmpty()) {
            toolsList.joinToString(", ")
        } else {
            "-"
        }
        binding.tvPackageEquipment.text = toolsText

        // Correctly fetch tools string from our helper function using the package name
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
                val packages = SupabaseClient.getCabangPackagesForClinic(prefs.clinicId)
                val matchedPkg = packages.firstOrNull { it.id == trx.cabang_package_id }
                if (matchedPkg != null) {
                    val correctToolsStr = matchedPkg.packages?.tools?.joinToString(", ") ?: ""
                    binding.tvPackageEquipment.text = if (correctToolsStr.isNotBlank()) correctToolsStr else toolsText
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        binding.tvTotalPrice.text = rupiahFormat.format(trx.total_amount)
    }

    private fun processCashPayment() {
        val trx = currentTransaction ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Pembayaran Tunai")
            .setMessage("Apakah Anda yakin ingin menyelesaikan transaksi ini secara Tunai?")
            .setPositiveButton("Ya") { dialog, _ ->
                dialog.dismiss()
                updatePaymentStatusToSuccess("success")
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updatePaymentStatusToSuccess(status: String) {
        binding.pbLoading.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Update Supabase payment status
                SupabaseClient.client.from("transactions").update(
                    buildJsonObject {
                        put("payment_status", status)
                    }
                ) {
                    filter { eq("id", transactionId) }
                }

                if (status == "success") {
                    SupabaseClient.checkAndUpdateActiveAppointment(requireContext())
                    val diagId = currentTransaction?.diagnosis_id
                    if (diagId != null) {
                        SupabaseClient.client.from("diagnosis").update(
                            buildJsonObject {
                                put("status", "Selesai")
                            }
                        ) {
                            filter { eq("id", diagId) }
                        }
                        
                        SupabaseClient.client.from("patient_progress").update(
                            buildJsonObject {
                                put("status", "Selesai")
                            }
                        ) {
                            filter { 
                                eq("diagnosis_id", diagId) 
                                eq("status", "Proses")
                            }
                        }
                    }
                }

                // Show success status dialog
                val successDialog = AlertDialog.Builder(requireContext())
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
                tvStatusTitle?.text = "Pembayaran Tunai Berhasil"
                tvStatusMessage?.text = "Layanan terapi dinyatakan Selesai."

                kotlinx.coroutines.delay(3000)
                successDialog.dismiss()

                // Go back to patient detail fragment
                findNavController().popBackStack()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memproses pembayaran: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun processCashlessPayment() {
        val trx = currentTransaction ?: return
        binding.pbLoading.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!trx.xendit_id.isNullOrBlank()) {
                    val invoiceUrl = "https://checkout.xendit.co/web/invoices/${trx.xendit_id}"
                    navigateToPaymentFragment(invoiceUrl)
                } else {
                    val email = trx.patients?.phone ?: "pasien@fisionet.com"
                    val desc = "Terapi ${trx.cabang_package?.packages?.name} - ${trx.patients?.name}"
                    
                    val result = xenditRepository.createInvoice(
                        transactionId = transactionId,
                        amount = trx.total_amount,
                        payerEmail = if (email.contains("@")) email else "$email@fisionet.com",
                        description = desc
                    )

                    // Update transaction in Supabase with xendit_id
                    SupabaseClient.client.from("transactions").update(
                        buildJsonObject {
                            put("xendit_id", result.invoiceId)
                        }
                    ) {
                        filter { eq("id", transactionId) }
                    }

                    navigateToPaymentFragment(result.invoiceUrl)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal membuat invoice online: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun navigateToPaymentFragment(url: String) {
        val bundle = Bundle().apply {
            putString("paymentUrl", url)
            putInt("transactionId", transactionId)
        }
        findNavController().navigate(R.id.action_receipt_to_payment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
