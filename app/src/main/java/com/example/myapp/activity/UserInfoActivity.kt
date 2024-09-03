package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.fragment.HomeFragment
import com.example.myapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserInfoActivity : AppCompatActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        initToolbar()
        initUi()
        updateStatusButton()
    }

    private fun updateStatusButton() {
        // Kiểm tra trạng thái nút xóa ảnh đại diện (Nếu không phải avtdf.jpg thì đổi màu đỏ)
        val currentUser = firebaseAuth.currentUser
        val btnDeleteAvatar = findViewById<Button>(R.id.btn_delete_avatar)
        if (currentUser != null) {
            val uid = currentUser.uid
            firestore.collection("Users").document(uid)
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val imageURL = documentSnapshot.getString("imageURL")
                        if (imageURL != null) {
                            if (imageURL.contains("avatardf.jpg")) {
                                // Nếu URL là "avatardf.jpg", làm cho nút không màu đỏ
                                btnDeleteAvatar?.apply {
                                    setBackgroundTintList(
                                        ContextCompat.getColorStateList(context, R.color.colorAccent)  // Chọn màu mặc định hoặc khác
                                    )
                                }
                            }else{
                                // Nếu URL khác, làm cho nút có màu đỏ
                                btnDeleteAvatar?.apply {
                                    setBackgroundTintList(
                                        ContextCompat.getColorStateList(context, R.color.red)
                                    )
                                }
                            }
                        }
                    }
                }

        }

        //Kiểm tra trạng thái nút lưu thông tin cá nhân (Nếu thông tin không đầy đủ thì không cho lưu)

    }

    private fun initToolbar() {
        val imgBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvTitle = findViewById<TextView>(R.id.tv_toolbar_title)

        // Thiết lập tiêu đề cho Toolbar
        tvTitle.text = getString(R.string.user_info_title)

        // Xử lý sự kiện khi click vào nút back
        imgBack.setOnClickListener {
            finish()
        }
    }

    private fun initUi() {
        val civAvatar = findViewById<ImageView>(R.id.civ_avatar)
        val edtName = findViewById<TextView>(R.id.edt_name)
        val edtPhone = findViewById<TextView>(R.id.edt_phone)
        val edtAddress = findViewById<TextView>(R.id.edt_address)

        // Lấy thông tin người dùng từ uid tương ứng và điền vào textview
        getUserInfo { name, phone, address, imageURL ->
            edtName?.text = name ?: getString(R.string.default_name)
            edtPhone?.text = phone ?: getString(R.string.default_phone)
            edtAddress?.text = address ?: getString(R.string.default_address)
            // Load ảnh đại diện
            if (imageURL != null) {
                if (civAvatar != null) {
                    Glide.with(this)
                        .load(imageURL)
                        .circleCrop()
                        .into(civAvatar)
                }
            }
        }

    }

    private fun getUserInfo(callback: (name: String?, phone: String?, address: String?, imageURL: String?) -> Unit) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            firestore.collection("Users").document(uid)
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        callback(null, null, null, null)
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val name = documentSnapshot.getString("name")
                        val phone = documentSnapshot.getString("phone")
                        val address = documentSnapshot.getString("address")
                        val imageURL = documentSnapshot.getString("imageURL")
                        callback(name, phone, address, imageURL)
                    } else {
                        callback(null, null, null, null)
                    }
                }
        } else {
            callback(null, null, null, null)
        }
    }

}