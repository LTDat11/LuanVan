package com.example.myapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.OptionsAdminPagerAdapter
import com.example.myapp.model.OptionsAdmin
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MoreOptionsFragment : Fragment() {
    private var mView: View? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_more_options, container, false)

        viewPager = mView!!.findViewById(R.id.view_pager_category_options)
        tabLayout = mView!!.findViewById(R.id.tab_category_options)

        setupViewPager()

        return mView
    }

    private fun setupViewPager() {
        val optionsList = listOf(
            OptionsAdmin("Danh Sách Dịch Vụ", ListServicesAdminFragment()),
            OptionsAdmin("Gói Dịch Vụ", PackageServicesAdminFragment()),
            OptionsAdmin("Banner", BannerAdminFragment())
        )

        val adapter = OptionsAdminPagerAdapter(requireActivity(), optionsList)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = optionsList[position].title
        }.attach()

        // Add the listener for tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // Handle tab selected
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Handle tab unselected
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Handle tab reselected
            }
        })
    }

}