package com.project.fisionettest.ui.cashier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.databinding.FragmentTransactionHistoryBinding
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

class TransactionHistoryFragment : Fragment() {

    private var _binding: FragmentTransactionHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter
    private lateinit var prefs: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPreferences(requireContext())

        setupRecyclerView()
        loadTransactions()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { transaction ->
            if (transaction.id != null) {
                val bundle = Bundle().apply {
                    putInt("transactionId", transaction.id)
                }
                findNavController().navigate(
                    R.id.action_transactionHistoryFragment_to_transactionDetailFragment,
                    bundle
                )
            }
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter
    }

    private fun loadTransactions() {
        lifecycleScope.launch {
            try {
                // Jika ada patientId dari argument, tampilkan semua transaksi pasien itu
                // (tidak perlu filter cabang — dokter lain boleh lihat riwayat pasien lintas cabang)
                val patientId = arguments?.getInt("patientId", -1) ?: -1
                val clinic = prefs.clinic  // Cabang terapis yang sedang login

                val transactions = SupabaseClient.client
                    .from("transactions")
                    .select(columns = Columns.raw("*, patients(*), cabang_package(*, packages(*)), diagnosis(*)")) {
                        if (patientId != -1) {
                            // Dipanggil dari PatientDetail — tampilkan semua riwayat pasien itu
                            filter { eq("patient_id", patientId) }
                        } else if (!clinic.isNullOrBlank()) {
                            // Dipanggil dari CashierMenu — filter per cabang terapis
                            val clinicId = com.project.fisionettest.utils.ClinicMapper.toId(clinic) ?: 0
                            filter { eq("id_cabang", clinicId) }
                        }
                        // Jika keduanya tidak ada (fallback / admin), tampilkan semua
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<Transaction>()

                if (transactions.isEmpty()) {
                    binding.tvEmptyHistory.visibility = View.VISIBLE
                    binding.rvTransactions.visibility = View.GONE
                } else {
                    binding.tvEmptyHistory.visibility = View.GONE
                    binding.rvTransactions.visibility = View.VISIBLE
                    adapter.submitList(transactions)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
