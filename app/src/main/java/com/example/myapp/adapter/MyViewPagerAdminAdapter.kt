package com.example.myapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapp.fragment.AccountAdminFragment
import com.example.myapp.fragment.HomeAdminFragment
import com.example.myapp.fragment.MoreOptionsFragment
import com.example.myapp.fragment.OrderFragment
import com.example.myapp.fragment.VoucherFragment

class MyViewPagerAdminAdapter (fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 5
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> OrderFragment()
            2 -> MoreOptionsFragment()
            3 -> VoucherFragment()
            4 -> AccountAdminFragment()
            else -> HomeAdminFragment()
        }
    }

}