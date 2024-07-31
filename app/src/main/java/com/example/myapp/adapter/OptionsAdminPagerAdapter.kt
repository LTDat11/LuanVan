package com.example.myapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapp.model.OptionsAdmin

class OptionsAdminPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val optionsList: List<OptionsAdmin>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = optionsList.size

    override fun createFragment(position: Int): Fragment = optionsList[position].fragment
}
