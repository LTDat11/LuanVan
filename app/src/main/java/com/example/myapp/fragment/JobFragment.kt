package com.example.myapp.fragment

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.activity.SplashActivity
import com.example.myapp.adapter.OderStatusPagerAdapter
import com.example.myapp.model.OrderStatus
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JobFragment : Fragment() {
    private var mView: View? = null
    private lateinit var viewPagerOrder: ViewPager2
    private lateinit var tabOrder: TabLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_job, container, false)
        viewPagerOrder = mView!!.findViewById(R.id.view_pager_job)
        tabOrder = mView!!.findViewById(R.id.tab_job)
        initToolbar()

        setupViewPager()
        listenChange()

        return mView
    }

    private fun listenChange() {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        // Lắng nghe thay đổi của đơn hàng được phân công
        db.collection("orders")
            .whereEqualTo("id_technician", auth.currentUser?.uid)
            .whereEqualTo("status", "processing")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Xử lý lỗi nếu cần
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val count = snapshot.size()
                    updateBadge(0, count)

                    updateBadgeForBottomNav(count)

                }
            }
    }


    private fun updateBadgeForBottomNav(count: Int) {
        // Update badge for bottom navigation
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val badge = bottomNav?.getOrCreateBadge(R.id.nav_job)
        badge?.isVisible = count > 0
        badge?.number = count
    }

    private fun updateBadge(tabPosition: Int, count: Int) {
        val tab = tabOrder.getTabAt(tabPosition)
        tab?.orCreateBadge?.apply {
            isVisible = count > 0
            number = count
        }
    }


    private fun setupViewPager() {
        val statusList = listOf(
            OrderStatus("Được phân công", LabelProcessTechFragment()),
            OrderStatus("Hoàn thành", LabelDoneTechFragment())
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
        val tvToolbarTitle = mView?.findViewById<TextView>(R.id.tv_toolbar_title)
        tvToolbarTitle?.text = getString(R.string.nav_job)
    }
}