package com.project.fisionettest.ui.cashier

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.databinding.FragmentPaymentBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private var paymentUrl: String? = null
    private var transactionId: Int = -1
    private var statusDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        paymentUrl = arguments?.getString("paymentUrl")
        transactionId = arguments?.getInt("transactionId", -1) ?: -1

        if (paymentUrl.isNullOrBlank()) {
            Toast.makeText(requireContext(), "URL Pembayaran tidak valid", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Configure WebView
        binding.webViewPayment.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        binding.webViewPayment.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressLoading.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressLoading.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // Intercept deep link redirects
                if (url.startsWith("fisionet://payment-success")) {
                    val uri = Uri.parse(url)
                    val trxId = uri.getQueryParameter("trx_id")?.toIntOrNull() ?: transactionId
                    updatePaymentStatusAndNavigate(trxId, "success")
                    return true
                } else if (url.startsWith("fisionet://payment-failed")) {
                    val uri = Uri.parse(url)
                    val trxId = uri.getQueryParameter("trx_id")?.toIntOrNull() ?: transactionId
                    updatePaymentStatusAndNavigate(trxId, "failed")
                    return true
                }
                
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        binding.webViewPayment.loadUrl(paymentUrl!!)
    }

    private fun updatePaymentStatusAndNavigate(trxId: Int, status: String) {
        if (view == null || _binding == null) return

        // Disable UI interactions during update
        binding.webViewPayment.visibility = View.GONE
        binding.progressLoading.visibility = View.VISIBLE

        // Show Custom Status Dialog
        statusDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_payment_status)
            .setCancelable(false)
            .create()

        statusDialog?.show()
        statusDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val pbLoading = statusDialog?.findViewById<android.widget.ProgressBar>(R.id.pb_loading)
        val ivStatusIcon = statusDialog?.findViewById<android.widget.ImageView>(R.id.iv_status_icon)
        val tvStatusTitle = statusDialog?.findViewById<android.widget.TextView>(R.id.tv_status_title)
        val tvStatusMessage = statusDialog?.findViewById<android.widget.TextView>(R.id.tv_status_message)
        val layoutActions = statusDialog?.findViewById<android.view.View>(R.id.layout_actions)
        val btnToHistory = statusDialog?.findViewById<android.view.View>(R.id.btn_to_history)
        val btnToHome = statusDialog?.findViewById<android.view.View>(R.id.btn_to_home)
        val btnCloseDialog = statusDialog?.findViewById<android.view.View>(R.id.btn_close_dialog)

        btnCloseDialog?.setOnClickListener {
            statusDialog?.dismiss()
        }

        lifecycleScope.launch {
            var isSuccessUpdate = false
            try {
                if (trxId != -1) {
                    val transaction = SupabaseClient.client.from("transactions").select {
                        filter { eq("id", trxId) }
                    }.decodeList<com.project.fisionettest.data.model.Transaction>().firstOrNull()

                    SupabaseClient.client.from("transactions").update(
                        buildJsonObject {
                            put("payment_status", status)
                        }
                    ) {
                        filter { eq("id", trxId) }
                    }

                    val diagId = transaction?.diagnosis_id
                    if (status == "success" && diagId != null) {
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
                    isSuccessUpdate = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (isAdded && _binding != null) {
                    binding.progressLoading.visibility = View.GONE

                    pbLoading?.visibility = View.GONE
                    ivStatusIcon?.visibility = View.VISIBLE
                    layoutActions?.visibility = View.VISIBLE

                    val prefs = com.project.fisionettest.utils.AppPreferences(requireContext())
                    val isAdmin = prefs.userRole == 1
                    val homeDestId = if (isAdmin) R.id.adminDashboardFragment else R.id.dashboardFragment

                    if (status == "success" && isSuccessUpdate) {
                        // Success State
                        ivStatusIcon?.setImageResource(android.R.drawable.checkbox_on_background)
                        ivStatusIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_present)
                        )
                        tvStatusTitle?.text = "Transaksi Berhasil"
                        tvStatusMessage?.text = "Pembayaran telah berhasil diverifikasi dan status transaksi diperbarui!"

                        btnToHistory?.setOnClickListener {
                            statusDialog?.dismiss()
                            if (isAdded) {
                                val bundle = Bundle().apply {
                                    putInt("transactionId", trxId)
                                }
                                val popped = findNavController().popBackStack(R.id.transactionHistoryFragment, false)
                                if (!popped) {
                                    findNavController().navigate(R.id.action_paymentFragment_to_transactionHistoryFragment, bundle)
                                }
                            }
                        }
                    } else {
                        // Failed State
                        ivStatusIcon?.setImageResource(android.R.drawable.ic_delete)
                        ivStatusIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_absent)
                        )
                        tvStatusTitle?.text = "Transaksi Gagal"
                        tvStatusMessage?.text = if (!isSuccessUpdate) {
                            "Gagal memperbarui status transaksi di server."
                        } else {
                            "Pembayaran dibatalkan atau gagal diproses."
                        }

                        btnToHistory?.setOnClickListener {
                            statusDialog?.dismiss()
                            if (isAdded) {
                                val bundle = Bundle().apply {
                                    putInt("transactionId", trxId)
                                }
                                val popped = findNavController().popBackStack(R.id.transactionHistoryFragment, false)
                                if (!popped) {
                                    findNavController().navigate(R.id.action_paymentFragment_to_transactionHistoryFragment, bundle)
                                }
                            }
                        }
                    }

                    btnToHome?.setOnClickListener {
                        statusDialog?.dismiss()
                        if (isAdded) {
                            val popped = findNavController().popBackStack(homeDestId, false)
                            if (!popped) {
                                findNavController().navigate(homeDestId)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        if (_binding != null) {
            binding.webViewPayment.webViewClient = android.webkit.WebViewClient()
            binding.webViewPayment.destroy()
        }
        statusDialog?.dismiss()
        statusDialog = null
        super.onDestroyView()
        _binding = null
    }
}
