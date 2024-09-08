package com.example.myapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapp.fragment.AccountFragment
import com.example.myapp.fragment.JobFragment

class MyViewPagerTechnicainAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> AccountFragment()
            else -> JobFragment()
        }
    }
}