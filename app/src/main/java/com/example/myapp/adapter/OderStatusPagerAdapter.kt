package com.example.myapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapp.model.OptionsAdmin
import com.example.myapp.model.OrderStatus

class OderStatusPagerAdapter (
    fragmentActivity: FragmentActivity,
    private val optionsList: List<OrderStatus>): FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = optionsList.size

    override fun createFragment(position: Int): Fragment = optionsList[position].fragment

}