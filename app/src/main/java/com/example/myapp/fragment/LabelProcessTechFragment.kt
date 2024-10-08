package com.example.myapp.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapter.JobAdapter
import com.example.myapp.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LabelProcessTechFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var jobAdapter: JobAdapter
    private lateinit var noDataTextView: TextView
    private val jobs = mutableListOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.fragment_label_process_tech, container, false)
        noDataTextView = mView.findViewById(R.id.tv_no_data)
        recyclerView = mView.findViewById(R.id.rcv_label_process_tech)
        recyclerView.layoutManager = LinearLayoutManager(context)
        jobAdapter = JobAdapter(jobs)
        recyclerView.adapter = jobAdapter

        loadJobs()
        // Inflate the layout for this fragment
        return mView

    }

    private fun loadJobs() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("orders")
            .whereEqualTo("id_technician", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Xử lý lỗi nếu cần
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    jobs.clear()
                    for (document in snapshot.documents) {
                        val order = document.toObject(Order::class.java)
                        if (order != null && order.status == "processing") { // Kiểm tra trạng thái
                            jobs.add(order)
                        }
                    }
                    jobAdapter.notifyDataSetChanged()

                    // Kiểm tra nếu danh sách trống sau khi tải dữ liệu
                    checkDataVisibility(jobs.isEmpty())
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