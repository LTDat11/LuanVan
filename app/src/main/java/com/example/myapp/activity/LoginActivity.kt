package com.example.myapp.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.myapp.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : BaseActivity()  {

    private var edtEmail: EditText? = null
    private var edtPassword: EditText? = null
    private var btnLogin: Button? = null
    private var layoutRegister: LinearLayout? = null
    private var tvForgotPassword: TextView? = null
    private var imgTogglePassword: ImageView? = null
    private var isEnableButtonLogin = false
    private var isPasswordVisible = false

    private val db: FirebaseFirestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initUi()
        initListener()
        updateUIState()
    }

    private fun initUi() {
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnLogin = findViewById(R.id.btn_login)
        layoutRegister = findViewById(R.id.layout_register)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
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
        layoutRegister?.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        btnLogin?.setOnClickListener { onClickValidateLogin() }
        tvForgotPassword?.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

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

        isEnableButtonLogin = isEmailValid && isPasswordValid
//        btnLogin?.setBackgroundResource(if (isEnableButtonLogin) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16)
        // Cập nhật nút MaterialButton
        btnLogin?.apply {
            background = ContextCompat.getDrawable(
                context,
                if (isEnableButtonLogin) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16
            )
            isEnabled = isEnableButtonLogin
        }
    }

    private fun onClickValidateLogin() {
        if (!isEnableButtonLogin) return
        val strEmail = edtEmail?.text.toString().trim()
        val strPassword = edtPassword?.text.toString().trim()

        when {
            strEmail.isEmpty() -> showToastMessage(getString(R.string.msg_email_require))
            strPassword.isEmpty() -> showToastMessage(getString(R.string.msg_password_require))
            !Patterns.EMAIL_ADDRESS.matcher(strEmail).matches() -> showToastMessage(getString(R.string.msg_email_invalid))
            else -> loginUserFirebase(strEmail, strPassword)
        }
    }

    private fun loginUserFirebase(email: String, password: String) {
        showProgressDialog(true)
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task: Task<AuthResult?> ->
                showProgressDialog(false)
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        val uid = user.uid
                        val userDocRef = db.collection("Users").document(uid)
                        userDocRef.get()
                            .addOnSuccessListener { document ->
                                if (document != null) {
                                    val role = document.getString("role")
                                    if (role == "Customer") {
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                            startActivity(intent)
                                    } else if (role == "Technician") {
                                            val intent = Intent(this, TechnicianActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                            startActivity(intent)
                                    }
                                    else {
                                        val intent = Intent(this, AdminActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                        startActivity(intent)
                                    }
                                } else {
                                    showToastMessage(getString(R.string.msg_login_error))

                                }
                            }
                            .addOnFailureListener { e ->
                                showToastMessage(getString(R.string.msg_login_error))
                            }
                    }
                } else {
                    showToastMessage(getString(R.string.msg_login_error))
                }
            }
    }
}