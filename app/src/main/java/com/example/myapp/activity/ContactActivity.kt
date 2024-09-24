package com.example.myapp.activity

import android.content.Intent
import android.net.Uri
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
        val contactAdapter = ContactAdapter(contactList){contactItem ->
            // Xử lý sự kiện khi click vào một item
            // Ví dụ: mở một trang web tương ứng với mỗi item
            when (contactItem.name) {
                "Điện thoại" -> {
                    // Mở ứng dụng điện thoại
                    val phoneIntent = Intent(Intent.ACTION_DIAL)
                    phoneIntent.data = Uri.parse("tel:0363897083")
                    startActivity(phoneIntent)
                }
                "Facebook" -> {
                    // Mở ứng dụng Facebook
                    val facebookIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/lutandat2k2/?locale=vi_VN"))
                    startActivity(facebookIntent)
                }
                "Zalo" -> {
                    // Mở ứng dụng Zalo
                    val zaloIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/0363897083"))
                    startActivity(zaloIntent)
                }
                "Instagram" -> {
                    // Mở ứng dụng Instagram
                    val instagramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/_dat.lu/"))
                    startActivity(instagramIntent)
                }
                "Mail" -> {
                    // Mở ứng dụng Mail
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:datb2004718@student.ctu.edu.vn")
                        putExtra(Intent.EXTRA_SUBJECT, "Contact Subject")
                    }
                    startActivity(emailIntent)
                }
            }
        }
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