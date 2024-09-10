package com.example.myapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.adapter.JobAdapter
import com.example.myapp.model.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LabelDoneTechFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var jobAdapter: JobAdapter
    private val jobs = mutableListOf<Order>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.fragment_label_done_tech, container, false)
        recyclerView = mView.findViewById(R.id.rcv_label_done_tech)
        recyclerView.layoutManager = LinearLayoutManager(context)
        jobAdapter = JobAdapter(jobs)
        recyclerView.adapter = jobAdapter

        loadJobs()

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
                        if (order != null && (order.status == "completed" || order.status == "finish")) { // Kiểm tra trạng thái
                            jobs.add(order)
                        }
                    }
                    jobAdapter.notifyDataSetChanged()
                }
            }
    }

}