package com.example.myapp.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.example.myapp.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.auth.User

class RegisterActivity : BaseActivity() {

    private var edtEmail: EditText? = null
    private var edtPassword: EditText? = null
    private var btnRegister: Button? = null
    private var layoutLogin: LinearLayout? = null
    private var isEnableButtonRegister = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initUi()
        initListener()

    }

    private fun initUi() {
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnRegister = findViewById(R.id.btn_register)
        layoutLogin = findViewById(R.id.layout_login)
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
        btnRegister?.setBackgroundResource(if (isEnableButtonRegister) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16)
    }

    private fun onClickValidateRegister() {
        if (!isEnableButtonRegister) return
        val strEmail = edtEmail?.text.toString().trim()
        val strPassword = edtPassword?.text.toString().trim()

        when {
            strEmail.isEmpty() -> showToastMessage(getString(R.string.msg_email_require))
            strPassword.isEmpty() -> showToastMessage(getString(R.string.msg_password_require))
            !Patterns.EMAIL_ADDRESS.matcher(strEmail).matches() -> showToastMessage(getString(R.string.msg_email_invalid))
//            else -> registerUserFirebase(strEmail, strPassword)
        }
    }

//    private fun registerUserFirebase(email: String, password: String) {
//        showProgressDialog(true)
//        val firebaseAuth = FirebaseAuth.getInstance()
//        firebaseAuth.createUserWithEmailAndPassword(email, password)
//            .addOnCompleteListener(this) { task: Task<AuthResult?> ->
//                showProgressDialog(false)
//                if (task.isSuccessful) {
//                    val user = firebaseAuth.currentUser
//                    if (user != null) {
//                        val userObject = User(user.email, password)
//                        DataStoreManager.user = userObject
//                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
//                        finishAffinity()
//                    }
//                } else {
//                    showToastMessage(getString(R.string.msg_register_error))
//                }
//            }
//    }

}