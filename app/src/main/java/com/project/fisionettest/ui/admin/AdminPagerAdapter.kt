package com.project.fisionettest.ui.admin

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.project.fisionettest.ui.cashier.PackageListFragment

class AdminPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PendingTherapistFragment()
            1 -> ApprovedTherapistFragment()
            2 -> AdminStatsFragment()
            3 -> AdminPatientsFragment()
            4 -> PackageListFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
