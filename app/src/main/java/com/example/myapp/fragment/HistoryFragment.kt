package com.example.myapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.activity.MainActivity
import com.example.myapp.adapter.OderStatusPagerAdapter
import com.example.myapp.model.OrderStatus
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HistoryFragment : Fragment() {
    private var mView: View? = null
    private lateinit var viewPagerOrder: ViewPager2
    private lateinit var tabOrder: TabLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_history, container, false)
        viewPagerOrder = mView!!.findViewById(R.id.view_pager_order)
        tabOrder = mView!!.findViewById(R.id.tab_order)
        initToolbar()

        setupViewPager()
        // Inflate the layout for this fragment
        return mView
    }

    private fun setupViewPager() {
        val statusList = listOf(
            OrderStatus("Đang xử lý", LabelProcessFragment()),
            OrderStatus("Hoàn thành", LabelDoneFragment())
        )
        val adapter = OderStatusPagerAdapter(requireActivity(), statusList)
        viewPagerOrder.adapter = adapter
        TabLayoutMediator(tabOrder, viewPagerOrder) { tab, position ->
            tab.text = statusList[position].title
        }.attach()

        // Add the listener for tab selection
        tabOrder.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // Handle tab selected
                viewPagerOrder.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Handle tab unselected
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Handle tab reselected
            }
        })
    }

    private fun initToolbar() {
        val imgToolbarBack = mView?.findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = mView?.findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack?.setOnClickListener { backToHomeScreen(HomeFragment()) }
        tvToolbarTitle?.text = getString(R.string.nav_history)
    }

    private fun backToHomeScreen(homeFragment: HomeFragment) {
        val mainActivity = activity as MainActivity? ?: return
        mainActivity.viewPager2?.currentItem = 0
    }

}
