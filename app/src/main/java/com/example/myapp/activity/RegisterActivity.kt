package com.example.myapp.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import com.example.myapp.R
import com.example.myapp.model.ApiResponse
import com.example.myapp.model.CheckEmailRequest
import com.example.myapp.model.CheckEmailResponse
import com.example.myapp.model.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.myapp.model.User
import com.example.myapp.model.UserRequest
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : BaseActivity() {

    private var edtEmail: EditText? = null
    private var edtPassword: EditText? = null
    private var btnRegister: Button? = null
    private var layoutLogin: LinearLayout? = null
    private var radioGroupRole: RadioGroup? = null
    private var imgTogglePassword: ImageView? = null
    private var isEnableButtonRegister = false
    private var isPasswordVisible = false
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
        imgTogglePassword = findViewById(R.id.img_toggle_password)
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

        imgTogglePassword?.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            edtPassword?.let {
                val selection = it.selectionStart
                it.transformationMethod = if (isPasswordVisible) null else android.text.method.PasswordTransformationMethod.getInstance()
                it.setSelection(selection)
            }
            imgTogglePassword?.setImageResource(if (isPasswordVisible) R.drawable.baseline_visibility_24 else R.drawable.baseline_visibility_off_24)
        }

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
            else -> checkEmail(strEmail, strPassword)
        }
    }

//   Register with firebase auth
    private fun registerUserFirebase(email: String, password: String) {
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
                        R.id.radio_admin -> "Admin"
                        else -> "Customer"
                    }
                    // Lấy url của avatardf.jpg trong thư mục avatar storage
                    val storage = FirebaseStorage.getInstance()
                    val avatarRef = storage.reference.child("avatar/avatardf.jpg")
                    avatarRef.downloadUrl.addOnSuccessListener { uri ->
                        val avatarUrl = uri.toString()
                        // Tạo một bản ghi trong Firestore
                        val db = FirebaseFirestore.getInstance()
                        val user = userId?.let {
                            User(
                                id = it,
                                email = email,
                                createdAt = currentTime,
                                updatedAt = currentTime,
                                role = selectedRole,
                                imageURL = avatarUrl
                            )
                        }

                        // Thêm thông tin user vào collection "Users" document với userId là key
                        userId?.let {
                            if (user != null) {
                                db.collection("Users").document(it)
                                    .set(user)
                                    .addOnSuccessListener {
                                        // Thêm UID vào collection tương ứng
                                        //addUserToRoleSpecificCollection(userId, selectedRole)
                                        // Gọi API để thêm custom claims
                                        setCustomClaims(userId)
                                        // Kiểm tra role tương ứng để chuyển hướng
                                        when (selectedRole) {
                                            "Customer" -> {
                                                //val intent = Intent(this, MainActivity::class.java)
                                                val intent = Intent(this, OnboardingActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                                startActivity(intent)
                                            }

                                            "Technician" -> {
                                                //val intent = Intent(this, TechnicianActivity::class.java)
                                                val intent = Intent(this, OnboardingActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                                startActivity(intent)
                                            }

                                            else -> {
                                                //val intent = Intent(this, AdminActivity::class.java)
                                                val intent = Intent(this, OnboardingActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                                startActivity(intent)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        showToastMessage("Failed to add user to Firestore: ${e.message}")
                                    }
                            }
                        }

                    }.addOnFailureListener { exception ->
                        Log.e("FirebaseStorage", "Error getting avatar URL", exception)
                    }

                }
            }
            .addOnFailureListener { exception ->
                showProgressDialog(false)
//                showToastMessage("Registration failed: ${exception.message}")
                Log.e("Registration", "Registration failed: ${exception.message}", exception)
            }
    }

    private fun checkEmail(email: String, password: String){
        val request = CheckEmailRequest(email)
        showProgressDialog(true)
        RetrofitInstance.api.checkEmail(request).enqueue(object : Callback<CheckEmailResponse> {
            override fun onResponse(call: Call<CheckEmailResponse>, response: Response<CheckEmailResponse>) {
                if (response.isSuccessful) {
                    val checkEmailResponse = response.body()
                    if (checkEmailResponse != null){
                        if (checkEmailResponse.registered){
                            showProgressDialog(false)
                            showToastMessage("Email đã được sử dụng!, vui lòng chọn email khác")
                        } else {
                            registerUserFirebase(email, password)
                        }
                    }
                } else {
                    showProgressDialog(false)
                    showToastMessage("Lỗi từ server: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<CheckEmailResponse>, t: Throwable) {
                showProgressDialog(false)
                Log.e("CheckEmail", "Error: ${t.message}")
            }
        })
    }

    private fun setCustomClaims(uid: String) {
        val request = UserRequest(uid)
        // Gọi API set custom claims
        RetrofitInstance.api.setCustomClaims(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Log.d("CustomClaims", "Custom claims set successfully")
                } else {
                    Log.e("CustomClaims", "Failed to set custom claims: ${response.errorBody()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("CustomClaims", "Error: ${t.message}")
            }
        })
    }

    // Thêm UID vào collection tương ứng
    private fun addUserToRoleSpecificCollection(userId: String, role: String) {
        val db = FirebaseFirestore.getInstance()

        // Lấy FCM Token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result

                // Chuẩn bị dữ liệu để lưu vào Firestore
                val userData = mapOf(
                    "userId" to userId,
                    "fcmToken" to fcmToken
                )

                when (role) {
                    "Customer" -> {
                        db.collection("Customers").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                // UID và FCM Token đã được thêm vào collection "Customers"
                            }
                            .addOnFailureListener { e ->
                                showToastMessage("Failed to add to Customers collection: ${e.message}")
                            }
                    }
                    "Technician" -> {
                        db.collection("Technicians").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                // UID và FCM Token đã được thêm vào collection "Technicians"
                            }
                            .addOnFailureListener { e ->
                                showToastMessage("Failed to add to Technicians collection: ${e.message}")
                            }
                    }
                }
            } else {
                showToastMessage("Failed to retrieve FCM Token: ${task.exception?.message}")
            }
        }
    }


}