package com.project.fisionettest.ui.cashier

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.data.model.Package
import com.project.fisionettest.databinding.ItemPackageBinding
import java.text.NumberFormat
import java.util.Locale

class PackageAdapter : ListAdapter<Package, PackageAdapter.PackageViewHolder>(PackageDiffCallback()) {

    var onEditClick: ((Package) -> Unit)? = null
    var onDeleteClick: ((Package) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = ItemPackageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PackageViewHolder(private val binding: ItemPackageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Package) {
            binding.tvPackageName.text = item.name
            
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            binding.tvPackagePrice.text = format.format(item.price)

            binding.tvToolsSummary.text = if (item.tools.isNotEmpty()) "Alat: ${item.tools.joinToString(", ")}" else "Alat: -"

            binding.btnEdit.setOnClickListener {
                onEditClick?.invoke(item)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick?.invoke(item)
            }
        }
    }

    class PackageDiffCallback : DiffUtil.ItemCallback<Package>() {
        override fun areItemsTheSame(oldItem: Package, newItem: Package): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Package, newItem: Package): Boolean {
            return oldItem == newItem
        }
    }
}
