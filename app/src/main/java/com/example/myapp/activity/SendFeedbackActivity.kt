package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.myapp.R
import com.example.myapp.databinding.ActivitySendFeedbackBinding
import com.example.myapp.model.Feedback
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SendFeedbackActivity : AppCompatActivity() {
    lateinit var binding : ActivitySendFeedbackBinding
    private var name: String? = null
    private var phone: String? = null
    private var email: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getDataIntent()
        initToolbar()
        initListener()
    }

    private fun getDataIntent() {
        name = intent.getStringExtra("name")
        phone = intent.getStringExtra("phone")
        email = intent.getStringExtra("email")
    }

    private fun initListener() {
        binding.apply {
            tvSendFeedback.setOnClickListener {
                onClickSendFeedback()
            }
        }
    }

    private fun onClickSendFeedback() {
        binding.apply {
            val feedbackComment = edtComment.text.toString().trim()
            val createdAt = java.util.Date()
            val feedbackRating = ratingBar.rating
            if (feedbackComment.isEmpty() || feedbackRating == 0f) {
                showToast(getString(R.string.comment_require))
                return
            }

            val feedback = Feedback(
                name = name,
                phone = phone,
                email = email,
                comment = feedbackComment,
                rating = feedbackRating,  // Gán giá trị đánh giá
                createdAt = createdAt  // Lưu thời gian hiện tại
            )

            saveFeedbackToFirestore(feedback)
        }
    }

    private fun saveFeedbackToFirestore(feedback: Feedback) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val db = FirebaseFirestore.getInstance()
                db.collection("feedback")
                    .add(feedback)
                    .addOnSuccessListener {
                        showToast(getString(R.string.send_feedback_success))
                        finish()
                    }
                    .addOnFailureListener {
                        showToast(getString(R.string.send_feedback_fail))
                    }
            }
        }

    }

    private fun initToolbar() {
        val imgBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvTitle = findViewById<TextView>(R.id.tv_toolbar_title)

        // Thiết lập tiêu đề cho Toolbar
        tvTitle.text = getString(R.string.send_feedback)

        // Xử lý sự kiện khi click vào nút back
        imgBack.setOnClickListener {
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}