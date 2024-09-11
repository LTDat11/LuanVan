package com.example.myapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapp.fragment.AccountFragment
import com.example.myapp.fragment.CategoryAdminFragment
import com.example.myapp.fragment.HomeAdminFragment
import com.example.myapp.fragment.OrderFragment

class MyViewPagerAdminAdapter (fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> OrderFragment()
//            2 -> MoreOptionsFragment()
            2 -> CategoryAdminFragment()
            3 -> AccountFragment()
            else -> HomeAdminFragment()
        }
    }

}