package com.project.fisionettest.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.fisionettest.R
import com.project.fisionettest.data.model.Profile
import com.project.fisionettest.databinding.ItemApprovedTherapistBinding
import com.project.fisionettest.utils.ClinicMapper

class ApprovedTherapistAdapter(
    private var therapists: List<Profile>,
    private val onTransferBranch: (Profile) -> Unit = {},
    private val onToggleStatus: (Profile) -> Unit = {}
) : RecyclerView.Adapter<ApprovedTherapistAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemApprovedTherapistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: Profile) {
            val name = profile.displayName ?: "Tanpa Nama"
            binding.tvName.text = name
            binding.tvAvatarInitial.text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "T"
            binding.chipClinic.text = ClinicMapper.toName(profile.id_cabang)

            val isActive = profile.status == "verified"

            // ── Badge status ──────────────────────────────────────────────
            if (isActive) {
                binding.tvStatusBadge.text = "Aktif"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            } else {
                binding.tvStatusBadge.text = "Nonaktif"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_inactive)
            }

            // ── Tombol toggle status ──────────────────────────────────────
            if (isActive) {
                binding.btnToggleStatus.text = "Nonaktifkan"
                binding.btnToggleStatus.setBackgroundColor(Color.parseColor("#E53935"))
            } else {
                binding.btnToggleStatus.text = "Aktifkan"
                binding.btnToggleStatus.setBackgroundColor(Color.parseColor("#2E7D32"))
            }

            // ── Tombol pindah cabang: hanya aktif jika terapis aktif ──────
            binding.btnTransferBranch.isEnabled = isActive
            binding.btnTransferBranch.alpha = if (isActive) 1.0f else 0.4f

            binding.btnTransferBranch.setOnClickListener {
                onTransferBranch(profile)
            }
            binding.btnToggleStatus.setOnClickListener {
                onToggleStatus(profile)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApprovedTherapistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(therapists[position])
    }

    override fun getItemCount(): Int = therapists.size

    fun updateList(newList: List<Profile>) {
        therapists = newList
        notifyDataSetChanged()
    }
}
