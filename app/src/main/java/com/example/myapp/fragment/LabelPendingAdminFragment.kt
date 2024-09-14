package com.example.myapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapter.OrderAdapter
import com.example.myapp.adapter.OrderAdminAdapter
import com.example.myapp.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LabelPendingAdminFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAminAdapter: OrderAdminAdapter

    //search view for search order by name or id or status or package name
    private lateinit var searchView: SearchView

    private val orders = mutableListOf<Order>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.fragment_label_pending_admin, container, false)

        recyclerView = mView.findViewById(R.id.rcv_label_pending)
        recyclerView.layoutManager = LinearLayoutManager(context)

        orderAminAdapter = OrderAdminAdapter(orders)
        recyclerView.adapter = orderAminAdapter

        loadOrders()



        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpSearchView()
    }

    private fun setUpSearchView() {
        searchView = requireView().findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    searchOrder(newText)
                }
                return true
            }
        })
    }

    private fun searchOrder(query: String) {
        val filteredOrders = orders.filter { order ->
            order.id?.contains(query, ignoreCase = true) ?: false ||
                    order.namePackage?.contains(query, ignoreCase = true) ?: false ||
                    order.description?.contains(query, ignoreCase = true) ?: false
        }

        orderAminAdapter = OrderAdminAdapter(filteredOrders)
        recyclerView.adapter = orderAminAdapter
    }

    private fun loadOrders() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()

                db.collection("orders")
                    .whereEqualTo("status", "pending")
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
                            orderAminAdapter.notifyDataSetChanged()
                        }
                    }
            }
        }

    }

}