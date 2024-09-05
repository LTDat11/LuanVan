package com.example.myapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.fragment_label_process, container, false)

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
                        if (order != null) {
                            orders.add(order)
                        }
                    }
                    orderAdapter.notifyDataSetChanged()
                }
            }
    }

}