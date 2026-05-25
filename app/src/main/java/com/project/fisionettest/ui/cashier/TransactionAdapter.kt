package com.project.fisionettest.ui.cashier

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.R
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(private val onItemClick: (Transaction) -> Unit) :
    ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
        holder.itemView.setOnClickListener { onItemClick(transaction) }
    }

    class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            val ctx = binding.root.context
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

            binding.tvDate.text = transaction.date
            binding.tvPatientName.text = transaction.patients?.name ?: "Pasien tidak ditemukan"
            binding.tvPackageName.text = transaction.packages?.name ?: "-"
            binding.tvAmount.text = format.format(transaction.total_amount)

            // Therapist name
            if (!transaction.user_name.isNullOrBlank()) {
                binding.tvUserName.text = "Oleh: ${transaction.user_name}"
                binding.tvUserName.visibility = View.VISIBLE
            } else {
                binding.tvUserName.visibility = View.GONE
            }

            // Cabang
            if (!transaction.cabang.isNullOrBlank()) {
                binding.tvCabang.text = transaction.cabang
                binding.tvCabang.visibility = View.VISIBLE
            } else {
                binding.tvCabang.visibility = View.GONE
            }

            // ---- STATUS CHIP ----
            val status = transaction.payment_status?.lowercase()?.trim() ?: "pending"
            when (status) {
                "success", "settled", "paid" -> {
                    binding.tvPaymentStatus.text = "✓ Lunas"
                    binding.tvPaymentStatus.setTextColor(Color.parseColor("#1B5E20"))
                    binding.tvPaymentStatus.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_chip_status_success)
                }
                "expired", "failed" -> {
                    binding.tvPaymentStatus.text = "✕ Gagal"
                    binding.tvPaymentStatus.setTextColor(Color.parseColor("#B71C1C"))
                    binding.tvPaymentStatus.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_chip_status_failed)
                }
                else -> {
                    binding.tvPaymentStatus.text = "⏳ Menunggu"
                    binding.tvPaymentStatus.setTextColor(Color.parseColor("#E65100"))
                    binding.tvPaymentStatus.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_chip_status_pending)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem == newItem
    }
}
