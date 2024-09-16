package com.example.myapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.OderStatusPagerAdapter
import com.example.myapp.model.OrderStatus
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderFragment : Fragment() {
    private var mView: View? = null
    private lateinit var viewPagerOrder: ViewPager2
    private lateinit var tabOrder: TabLayout
    private val viewedOrders = mutableMapOf<String, List<String>>()

    // Danh sách trạng thái đơn hàng dùng chung
    private val statuses = listOf("pending", "processing", "completed", "finish")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_order, container, false)
        viewPagerOrder = mView!!.findViewById(R.id.view_pager_order)
        tabOrder = mView!!.findViewById(R.id.tab_order)

        initToolbar()
        setupViewPager()
        listenChange()

        return mView
    }

    private fun listenChange() {
        val db = FirebaseFirestore.getInstance()

        statuses.forEachIndexed { index, status ->
            db.collection("orders")
                .whereEqualTo("status", status)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener

                    val orderIds = snapshot.documents.map { it.id }
                    val previouslyViewed = viewedOrders[status] ?: emptyList()

                    if (status == "pending") {
                        // Luôn cập nhật badge cho "pending" với tổng số đơn hàng
                        updateBadge(index, orderIds.size)
                    } else {
                        // Chỉ cập nhật badge nếu có đơn hàng mới mà người dùng chưa xem
                        val newOrders = orderIds.filter { it !in previouslyViewed }
                        if (newOrders.isNotEmpty()) {
                            updateBadge(index, newOrders.size)  // Cập nhật badge với số đơn hàng mới
                            viewedOrders[status] = orderIds     // Lưu trạng thái mới đã xem
                        }
                    }
                }
        }
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
            OrderStatus("Chờ nhận đơn", LabelPendingAdminFragment()),
            OrderStatus("Đang xử lý", LabelProcessingAdminFragment()),
            OrderStatus("Chờ thanh toán", LabelCompleteAdminFragment()),
            OrderStatus("Đã thanh toán", LabelFinishAdminFragment())
        )

        val adapter = OderStatusPagerAdapter(requireActivity(), statusList)
        viewPagerOrder.adapter = adapter
        TabLayoutMediator(tabOrder, viewPagerOrder) { tab, position ->
            tab.text = statusList[position].title
        }.attach()

        tabOrder.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position != 0) {
                    val selectedStatus = statuses[tab.position]
                    viewedOrders[selectedStatus] = emptyList()
                    updateBadge(tab.position, 0)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun initToolbar() {
        val tvToolbarTitle = mView?.findViewById<TextView>(R.id.tv_toolbar_title)
        tvToolbarTitle?.text = getString(R.string.nav_order)
    }
}