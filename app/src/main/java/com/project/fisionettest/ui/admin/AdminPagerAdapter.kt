package com.project.fisionettest.ui.admin

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class AdminPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PendingTherapistFragment()
            1 -> ApprovedTherapistFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
