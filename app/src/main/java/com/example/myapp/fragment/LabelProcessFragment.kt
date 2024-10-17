package com.example.myapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapter.OrderAdapter
import com.example.myapp.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LabelProcessFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private val orders = mutableListOf<Order>()
    private lateinit var noDataTextView: TextView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.fragment_label_process, container, false)
        noDataTextView = mView.findViewById(R.id.tv_no_data)
        recyclerView = mView.findViewById(R.id.rcv_label_process)
        recyclerView.layoutManager = LinearLayoutManager(context)

        orderAdapter = OrderAdapter(orders)
        recyclerView.adapter = orderAdapter

        loadOrders()


        return mView
    }

    private fun loadOrders() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("orders")
            .whereEqualTo("id_customer", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Xử lý lỗi nếu cần
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    orders.clear()
                    for (document in snapshot.documents) {
                        val order = document.toObject(Order::class.java)
                        // Kiểm tra trạng thái đơn hàng finish hoặc cancel
                        if (order != null && order.status != "finish" && order.status != "cancel") {
                            orders.add(order)
                        }
                    }
                    orderAdapter.notifyDataSetChanged()

                    // Kiểm tra nếu danh sách trống sau khi tải dữ liệu
                    checkDataVisibility(orders.isEmpty())
                }
            }
    }

    private fun checkDataVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            noDataTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noDataTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

}