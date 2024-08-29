package com.example.myapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.myapp.R
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ForgotPasswordActivity : BaseActivity() {

    private var edtEmail: EditText? = null
    private var btnResetPassword: Button? = null
    private var isEnableButtonResetPassword = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initToolbar()
        initUi()
        initListener()
        updateUIState()
    }

    private fun initToolbar() {
        val imgToolbarBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack.setOnClickListener { finish() }
        tvToolbarTitle.text = getString(R.string.reset_password)
    }

    private fun initUi() {
        edtEmail = findViewById(R.id.edt_email)
        btnResetPassword = findViewById(R.id.btn_reset_password)
    }

    private fun initListener() {
        edtEmail?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                updateUIState()
            }
        })
        btnResetPassword?.setOnClickListener { onClickValidateResetPassword() }
    }

    private fun updateUIState() {
        val email = edtEmail?.text.toString().trim()
        val isEmailValid = email.isNotEmpty()

        edtEmail?.setBackgroundResource(if (isEmailValid) R.drawable.bg_white_corner_16_border_main else R.drawable.bg_white_corner_16_border_gray)
        isEnableButtonResetPassword = isEmailValid
//                btnResetPassword?.setBackgroundResource(if (isEnableButtonResetPassword) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16)
        btnResetPassword?.apply {
            background = ContextCompat.getDrawable(
                context,
                if (isEnableButtonResetPassword) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16
            )
            isEnabled = isEnableButtonResetPassword
        }
    }

    private fun onClickValidateResetPassword() {
        if (!isEnableButtonResetPassword) return

        val email = edtEmail?.text.toString().trim()
        when {
            email.isEmpty() -> showToastMessage(getString(R.string.msg_email_require))
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showToastMessage(getString(R.string.msg_email_invalid))
            else -> resetPassword(email)
        }
    }

    private fun resetPassword(email: String) {
        showProgressDialog(true)

        val db = FirebaseFirestore.getInstance()

        // Truy vấn tìm email trong collection Users
        db.collection("Users")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener { task ->
                showProgressDialog(false)

                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        // Email found, proceed with sending reset password email
                        sendResetEmail(email)
                    } else {
                        // Email not found in the Users collection
                        showToastMessage(getString(R.string.msg_email_not_registered))
                    }
                } else {
                    // Handle the error when querying Firestore
                    showToastMessage(getString(R.string.msg_reset_password_error))
                }
            }
    }

    // Function to send reset password email
    private fun sendResetEmail(email: String) {
        val auth = FirebaseAuth.getInstance()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showProgressDialog(false)
                if (task.isSuccessful) {
                    showToastMessage(getString(R.string.msg_reset_password_successfully))
                    edtEmail?.setText("")
                } else {
                    showToastMessage(getString(R.string.msg_reset_password_error))
                }
            }
    }

}