package com.example.myapp.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import com.example.myapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.myapp.model.User
import com.google.firebase.storage.FirebaseStorage

class RegisterActivity : BaseActivity() {

    private var edtEmail: EditText? = null
    private var edtPassword: EditText? = null
    private var btnRegister: Button? = null
    private var layoutLogin: LinearLayout? = null
    private var radioGroupRole: RadioGroup? = null
    private var isEnableButtonRegister = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initUi()
        initListener()
        updateUIState()
    }

    private fun initUi() {
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnRegister = findViewById(R.id.btn_register)
        layoutLogin = findViewById(R.id.layout_login)
        radioGroupRole = findViewById(R.id.radio_group_role)
    }

    private fun initListener() {
        edtEmail?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                updateUIState()
            }
        })
        edtPassword?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                updateUIState()
            }
        })
        layoutLogin?.setOnClickListener { finish() }
        btnRegister?.setOnClickListener { onClickValidateRegister() }
    }

    private fun updateUIState() {
        val email = edtEmail?.text.toString()
        val password = edtPassword?.text.toString()

        val isEmailValid = email.isNotEmpty()
        val isPasswordValid = password.isNotEmpty()

        edtEmail?.setBackgroundResource(if (isEmailValid) R.drawable.bg_white_corner_16_border_main else R.drawable.bg_white_corner_16_border_gray)
        edtPassword?.setBackgroundResource(if (isPasswordValid) R.drawable.bg_white_corner_16_border_main else R.drawable.bg_white_corner_16_border_gray)

        isEnableButtonRegister = isEmailValid && isPasswordValid
//        btnRegister?.setBackgroundResource(if (isEnableButtonRegister) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16)
        // Cập nhật nút MaterialButton
        btnRegister?.apply {
            background = ContextCompat.getDrawable(
                context,
                if (isEnableButtonRegister) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16
            )
            isEnabled = isEnableButtonRegister
        }
    }

    private fun onClickValidateRegister() {
        if (!isEnableButtonRegister) return
        val strEmail = edtEmail?.text.toString().trim()
        val strPassword = edtPassword?.text.toString().trim()

        when {
            strEmail.isEmpty() -> showToastMessage(getString(R.string.msg_email_require))
            strPassword.isEmpty() -> showToastMessage(getString(R.string.msg_password_require))
            !Patterns.EMAIL_ADDRESS.matcher(strEmail).matches() -> showToastMessage(getString(R.string.msg_email_invalid))
            else -> registerUserFirebase(strEmail, strPassword)
        }
    }

//   Register with firebase auth
    private fun registerUserFirebase(email: String, password: String) {
        showProgressDialog(true)
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener { task ->
                showProgressDialog(false)
                if (task.isSuccessful){
                    // Lấy UID của user vừa đăng ký
                    val userId = auth.currentUser?.uid
                    // Lấy thời gian hiện tại
                    val currentTime = java.util.Date()
                    // lấy role của user
                    val selectedRole = when (radioGroupRole?.checkedRadioButtonId) {
                        R.id.radio_customer -> "Customer"
                        R.id.radio_technician -> "Technician"
                        else -> "Admin"
                    }
                    // Lấy url của avatardf.jpg trong thư mục avatar storage
                    val storage = FirebaseStorage.getInstance()
                    val avatarRef = storage.reference.child("avatar/avatardf.jpg")
                    avatarRef.downloadUrl.addOnSuccessListener { uri ->
                        val avatarUrl = uri.toString()

                        // Tạo một bản ghi trong Firestore
                        val db = FirebaseFirestore.getInstance()
                        val user = User(
                            email = email,
                            createdAt = currentTime,
                            updatedAt = currentTime,
                            role = selectedRole,
                            imageURL = avatarUrl
                        )

                        // Thêm thông tin user vào collection "Users" document với userId là key
                        userId?.let {
                            db.collection("Users").document(it)
                                .set(user)
                                .addOnSuccessListener {
                                    // Thêm UID vào collection tương ứng
                                    addUserToRoleSpecificCollection(userId, selectedRole)

                                    // Kiểm tra role tương ứng để chuyển hướng
                                    when (selectedRole) {
                                        "Customer" -> {
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                            startActivity(intent)
                                        }
                                        "Technician" -> {
                                            // Uncomment this if you have a TechnicianActivity
                                            // val intent = Intent(this, TechnicianActivity::class.java)
                                            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                            // startActivity(intent)
                                        }
                                        else -> {
                                            val intent = Intent(this, AdminActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                            startActivity(intent)
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    showToastMessage("Failed to add user to Firestore: ${e.message}")
                                }
                        }

                    }.addOnFailureListener { exception ->
                        Log.e("FirebaseStorage", "Error getting avatar URL", exception)
                    }

                }
            }
            .addOnFailureListener { exception ->
                showProgressDialog(false)
                showToastMessage("Registration failed: ${exception.message}")
            }
    }

    // Thêm UID vào collection tương ứng
    private fun addUserToRoleSpecificCollection(userId: String, role: String) {
        val db = FirebaseFirestore.getInstance()
        when (role) {
            "Customer" -> {
                db.collection("Customers").document(userId)
                    .set(mapOf("userId" to userId))
                    .addOnSuccessListener {
                        // UID đã được thêm vào collection "Customers"
                    }
                    .addOnFailureListener { e ->
                        showToastMessage("Failed to add to Customers collection: ${e.message}")
                    }
            }
            "Technician" -> {
                db.collection("Technicians").document(userId)
                    .set(mapOf("userId" to userId))
                    .addOnSuccessListener {
                        // UID đã được thêm vào collection "Technicians"
                    }
                    .addOnFailureListener { e ->
                        showToastMessage("Failed to add to Technicians collection: ${e.message}")
                    }
            }
        }
    }

}