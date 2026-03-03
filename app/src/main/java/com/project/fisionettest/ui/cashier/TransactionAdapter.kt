package com.project.fisionettest.ui.cashier

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(private val onItemClick: (Transaction) -> Unit) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
    }

    class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            
            binding.tvDate.text = transaction.date
            binding.tvPatientName.text = transaction.patients?.name ?: "Pasien tidak ditemukan"
            
            // Derive package names from details
            val packageNames = transaction.packages?.name ?: "-"
            binding.tvPackageName.text = packageNames
            
            binding.tvAmount.text = format.format(transaction.total_amount)
            
            // Display therapist name
            if (!transaction.user_name.isNullOrBlank()) {
                 binding.tvUserName.text = transaction.user_name
                 binding.tvUserName.visibility = android.view.View.VISIBLE
            } else {
                 binding.tvUserName.visibility = android.view.View.GONE
            }

            // Display Cabang
            if (!transaction.cabang.isNullOrBlank()) {
                binding.tvCabang.text = transaction.cabang
                binding.tvCabang.visibility = android.view.View.VISIBLE
            } else {
                binding.tvCabang.visibility = android.view.View.GONE
            }

            // Display Payment Status
            binding.tvPaymentStatus.text = "Status Pembayaran: ${transaction.payment_status ?: "pending"}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
