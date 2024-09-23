package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapp.R
import com.example.myapp.adapter.ContactAdapter
import com.example.myapp.databinding.ActivityContactBinding
import com.example.myapp.model.ContactItem

class ContactActivity : AppCompatActivity() {
    lateinit var binding : ActivityContactBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo danh sách liên lạc
        val contactList = listOf(
            ContactItem("Điện thoại", R.drawable.ic_hotline),
            ContactItem("Facebook", R.drawable.ic_facebook),
            ContactItem("Zalo", R.drawable.ic_zalo),
            ContactItem("Instagram", R.drawable.ic_instagram),
            ContactItem("Mail", R.drawable.ic_gmail)
        )

        initToolbar()

        // Thiết lập RecyclerView
        val contactAdapter = ContactAdapter(contactList)
        binding.rvContact.apply {
            layoutManager = GridLayoutManager(this@ContactActivity, 3)
            adapter = contactAdapter
            setHasFixedSize(true)
        }
    }

    private fun initToolbar() {
        val imgBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvTitle = findViewById<TextView>(R.id.tv_toolbar_title)

        // Thiết lập tiêu đề cho Toolbar
        tvTitle.text = getString(R.string.infor_contact)

        // Xử lý sự kiện khi click vào nút back
        imgBack.setOnClickListener {
            finish()
        }
    }
}