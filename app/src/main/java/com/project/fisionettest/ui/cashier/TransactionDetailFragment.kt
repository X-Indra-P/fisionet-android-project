package com.project.fisionettest.ui.cashier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.databinding.FragmentTransactionDetailBinding
import com.project.fisionettest.R
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class TransactionDetailFragment : Fragment() {

    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!
    private var transactionId: Int? = null

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
    }

    private fun loadTransactionDetails(id: Int) {
        lifecycleScope.launch {
            try {
                // Fetch transaction with relations: patients, packages, diagnosis
                val transaction = SupabaseClient.client
                    .from("transactions")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, patients(*), packages(*), diagnosis(*)")) {
                        filter {
                            eq("id", id)
                        }
                    }
                    .decodeSingleOrNull<Transaction>()

                if (transaction != null) {
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

        binding.tvDate.text = transaction.date
        binding.tvPatientName.text = transaction.patients?.name ?: "Pasien tidak ditemukan"
        
        // Display Diagnosis Name
        // diagnosis relation should be populated now
        // Assuming Diagnosis has 'diagnosa' field based on previous files.
        binding.tvDiagnosis.text = "${transaction.diagnosis?.diagnosa ?: transaction.diagnosis_id ?: "-"}"
        
        // For Therapist, we need user name.
        binding.tvTherapist.text = transaction.user_name ?: transaction.user_id ?: "-"

        binding.tvTotalAmount.text = format.format(transaction.total_amount)

        // Details & Tools
        val pkg = transaction.packages
        binding.layoutPackageDetails.removeAllViews() // Clear placeholders

        if (pkg != null) {
            val pkgName = pkg.name
            val tools = if (pkg.tools.isNotEmpty()) pkg.tools.joinToString(", ") else "Tidak ada alat"

            // Create TextViews dynamically or inflate a simple layout
            // Simple TextView for now
            val textView = android.widget.TextView(requireContext())
            textView.text = "$pkgName\nAlat: $tools"
            textView.setTextColor(resources.getColor(R.color.black, null))
            textView.textSize = 14f
            textView.setPadding(0, 0, 0, 16)

            binding.layoutPackageDetails.addView(textView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
