package com.example.myapp.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
import java.util.Locale

class LabelCompleteAdminFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAminAdapter: OrderAdminAdapter
    private lateinit var noDataTextView: TextView
    private lateinit var searchView: SearchView
    private lateinit var imageButtonMicrophone: ImageButton
    private val orders = mutableListOf<Order>()
    private val REQUEST_CODE_VOICE_RECOGNITION = 100


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.fragment_label_complete_admin, container, false)
        noDataTextView = mView.findViewById(R.id.tv_no_data)
        imageButtonMicrophone = mView.findViewById(R.id.image_button_microphone)
        recyclerView = mView.findViewById(R.id.rcv_label_complete)
        recyclerView.layoutManager = LinearLayoutManager(context)
        orderAminAdapter = OrderAdminAdapter(orders)
        recyclerView.adapter = orderAminAdapter

        loadOrders()

        // Inflate the layout for this fragment
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpSearchView()
        initlisteners()
    }

    private fun initlisteners() {
        imageButtonMicrophone.setOnClickListener {
            openVoiceRecognizer()
        }
    }

    private fun openVoiceRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói điều gì đó...")
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE_RECOGNITION)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Không hỗ trợ nhận diện giọng nói.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_VOICE_RECOGNITION && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.let {
                if (it.isNotEmpty()) {
                    // Điền kết quả vào SearchView
                    searchView?.setQuery(it[0], false)
                }
            }
        }
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
                    order.namePackage?.contains(query, ignoreCase = true) ?: false
        }

        orderAminAdapter = OrderAdminAdapter(filteredOrders)
        recyclerView.adapter = orderAminAdapter

        // Kiểm tra nếu danh sách trống sau khi tìm kiếm
        checkDataVisibility(filteredOrders.isEmpty())
    }

    private fun loadOrders() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()

                db.collection("orders")
                    .whereEqualTo("status", "completed")
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

                            // Kiểm tra nếu danh sách trống sau khi tải dữ liệu
                           checkDataVisibility(orders.isEmpty())
                        }
                    }

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