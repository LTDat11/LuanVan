package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.R
import com.example.myapp.adapter.FeedbackAdapter
import com.example.myapp.databinding.ActivityViewFeedbackBinding
import com.example.myapp.model.Feedback
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewFeedbackActivity : AppCompatActivity() {
    lateinit var binding : ActivityViewFeedbackBinding
    private lateinit var feedbackAdapter: FeedbackAdapter
    private val feedbacks = mutableListOf<Feedback>()
    private val originalFeedbacks = mutableListOf<Feedback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initRecyclerView()
        loadFeedbacks()
        setupSearchView()
    }

    private fun setupSearchView() {
        binding.apply {
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrEmpty()) {
                        feedbackAdapter.updateList(originalFeedbacks) // Hiển thị lại danh sách gốc
                    } else {
                        val filteredFeedbacks = originalFeedbacks.filter {
                            it.name?.contains(newText, ignoreCase = true) ?: false ||
                                    it.email?.contains(newText, ignoreCase = true) ?: false ||
                                    it.comment?.contains(newText, ignoreCase = true) ?: false
                        }
                        feedbackAdapter.updateList(filteredFeedbacks) // Cập nhật danh sách đã lọc
                    }
                    return true
                }
            })
        }
    }

    private fun initRecyclerView() {
        feedbackAdapter = FeedbackAdapter(feedbacks)
        binding.recyclerViewFeedback.adapter = feedbackAdapter
        binding.recyclerViewFeedback.layoutManager = LinearLayoutManager(this)
    }


    private fun loadFeedbacks() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()
                db.collection("feedback")
                    .addSnapshotListener { querySnapshot, error ->
                        if (error != null) {
                            // Xử lý lỗi nếu có
                            return@addSnapshotListener
                        }

                        if (querySnapshot != null) {
                            feedbacks.clear()
                            originalFeedbacks.clear() // Xóa danh sách gốc trước khi thêm mới

                            for (document in querySnapshot.documents) {
                                val feedback = document.toObject(Feedback::class.java)
                                if (feedback != null) {
                                    feedbacks.add(feedback)
                                    originalFeedbacks.add(feedback) // Lưu feedback vào danh sách gốc
                                }
                            }

                            feedbackAdapter.notifyDataSetChanged() // Cập nhật lại giao diện
                        }
                    }
            }
        }

    }


    private fun initToolbar() {
        val imgBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvTitle = findViewById<TextView>(R.id.tv_toolbar_title)

        // Thiết lập tiêu đề cho Toolbar
        tvTitle.text = getString(R.string.view_feedback)

        // Xử lý sự kiện khi click vào nút back
        imgBack.setOnClickListener {
            finish()
        }
    }
}