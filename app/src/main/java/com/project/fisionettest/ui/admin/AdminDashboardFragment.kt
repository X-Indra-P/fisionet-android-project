package com.project.fisionettest.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.R
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.databinding.FragmentAdminDashboardBinding
import com.google.android.material.tabs.TabLayoutMediator
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.util.Locale

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdminHeader()
        setupViewPager()

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                SupabaseClient.client.auth.signOut()
                findNavController().navigate(R.id.action_adminDashboard_to_login)
            }
        }
    }

    private fun setupAdminHeader() {
        val user = SupabaseClient.client.auth.currentUserOrNull()
        if (user != null) {
            val metadata = user.userMetadata
            var displayName = user.email
                ?.substringBefore("@")
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                ?: "Admin"

            if (metadata != null && metadata.containsKey("display_name")) {
                displayName = metadata["display_name"].toString().replace("\"", "")
            }

            binding.tvAdminName.text = displayName
            binding.tvAvatarInitial.text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
        }
    }

    private fun setupViewPager() {
        val adapter = AdminPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "⏳  Menunggu"
                1 -> "✅  Disetujui"
                else -> ""
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
