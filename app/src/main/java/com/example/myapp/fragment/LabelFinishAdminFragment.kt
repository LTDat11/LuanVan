package com.example.myapp.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapter.OrderAdminAdapter
import com.example.myapp.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LabelFinishAdminFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAminAdapter: OrderAdminAdapter
    private lateinit var searchView: SearchView
    private val orders = mutableListOf<Order>()
    private var filteredOrders = mutableListOf<Order>()  // Danh sách tạm thời lưu trữ kết quả lọc
    private lateinit var tvFilterDate: TextView
    private lateinit var tvNoData: TextView
    private var selectedDate: Date? = null
    private var currentQuery: String = ""  // Lưu trữ từ khóa tìm kiếm hiện tại

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.fragment_label_finish_admin, container, false)
        tvNoData = mView.findViewById(R.id.tv_no_data)
        recyclerView = mView.findViewById(R.id.rcv_label_finish)
        recyclerView.layoutManager = LinearLayoutManager(context)
        orderAminAdapter = OrderAdminAdapter(filteredOrders)
        recyclerView.adapter = orderAminAdapter

        loadOrders()

        // Inflate the layout for this fragment
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpSearchView()
        tvFilterDate = view.findViewById(R.id.tv_filter_date)
        // Khởi tạo với ngày hiện tại
        val calendar = Calendar.getInstance()
        selectedDate = calendar.time // Ngày hiện tại
        // Hiển thị ngày hiện tại trong TextView
        tvFilterDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)
        tvFilterDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                selectedDate = calendar.time
                tvFilterDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)

                // Lọc đơn hàng theo ngày đã chọn
                filterOrders()
            }, year, month, day)

        datePickerDialog.show()
    }

    private fun setUpSearchView() {
        searchView = requireView().findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                // Lọc đơn hàng khi từ khóa tìm kiếm thay đổi
                filterOrders()
                return true
            }
        })
    }

    private fun filterOrders() {
        // Lọc theo từ khóa tìm kiếm
        val tempFilteredOrders = orders.filter { order ->
            order.id?.contains(currentQuery, ignoreCase = true) ?: false ||
                    order.namePackage?.contains(currentQuery, ignoreCase = true) ?: false
        }

        // Lọc theo ngày nếu ngày đã được chọn
        selectedDate?.let { date ->
            filteredOrders = tempFilteredOrders.filter { order ->
                order.createdAt?.let { createdAt ->
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(createdAt) == sdf.format(date)
                } ?: false
            }.toMutableList()
        } ?: run {
            // Nếu không chọn ngày, chỉ lọc theo từ khóa tìm kiếm
            filteredOrders = tempFilteredOrders.toMutableList()
        }

        // Cập nhật UI với kết quả lọc
        updateUI(filteredOrders)
    }

    private fun updateUI(orderList: List<Order>) {
        if (orderList.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvNoData.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvNoData.visibility = View.GONE
            orderAminAdapter = OrderAdminAdapter(orderList)
            recyclerView.adapter = orderAminAdapter
        }
    }

    private fun loadOrders() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()

                db.collection("orders")
                    .whereEqualTo("status", "finish")
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

                            // Cập nhật danh sách lọc khi dữ liệu thay đổi
                            filterOrders()
                        }
                    }

            }
        }
    }
}
