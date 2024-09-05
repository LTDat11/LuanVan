package com.example.myapp.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.myapp.R
import com.example.myapp.databinding.ActivityChangePassBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePassActivity : BaseActivity() {
    lateinit var binding: ActivityChangePassBinding
    private var isEnableButtonChangePass = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initListener()
    }

    private fun initListener() {
        binding.apply {
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    checkFieldsForEmptyValues()
                }
            }

            edtOldPassword.addTextChangedListener(textWatcher)
            edtNewPassword.addTextChangedListener(textWatcher)
            edtConfirmPassword.addTextChangedListener(textWatcher)

            btnChangePassword.setOnClickListener {
                onClickValidateChangePassword()
            }
        }
    }

    private fun checkFieldsForEmptyValues() {
        binding.apply {
            val strOldPassword = edtOldPassword?.text.toString().trim()
            val strNewPassword = edtNewPassword?.text.toString().trim()
            val strConfirmPassword = edtConfirmPassword?.text.toString().trim()

            // Kiểm tra các trường có trống không
            isEnableButtonChangePass = strOldPassword.isNotEmpty() && strNewPassword.isNotEmpty() && strConfirmPassword.isNotEmpty()

            // Cập nhật giao diện nút
            btnChangePassword.apply {
                background = ContextCompat.getDrawable(
                    context,
                    if (isEnableButtonChangePass) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16
                )
                isEnabled = isEnableButtonChangePass
            }
        }
    }

    private fun onClickValidateChangePassword() {
        binding.apply {
            val strOldPassword = edtOldPassword?.text.toString().trim { it <= ' ' }
            val strNewPassword = edtNewPassword?.text.toString().trim { it <= ' ' }
            val strConfirmPassword = edtConfirmPassword?.text.toString().trim { it <= ' ' }

            if (strOldPassword.isNullOrEmpty()) {
                showToastMessage(getString(R.string.msg_old_password_require))
            } else if (strNewPassword.isNullOrEmpty()) {
                showToastMessage(getString(R.string.msg_new_password_require))
            } else if (strConfirmPassword.isNullOrEmpty()) {
                showToastMessage(getString(R.string.msg_confirm_password_require))
            } else if (strNewPassword != strConfirmPassword) {
                showToastMessage(getString(R.string.msg_confirm_password_invalid))
            } else if (strOldPassword == strNewPassword) {
                showToastMessage(getString(R.string.msg_new_password_invalid))
            } else {
                reauthenticateAndChangePassword(strOldPassword, strNewPassword)
            }
        }
    }

    private fun reauthenticateAndChangePassword(oldPassword: String, newPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val email = user.email
            val credential = EmailAuthProvider.getCredential(email!!, oldPassword)

            user.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showProgressDialog(true)
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { updateTask ->
                                showProgressDialog(false)
                                if (updateTask.isSuccessful) {
                                    showToastMessage(getString(R.string.msg_password_changed_success))
                                    binding.apply {
                                        edtOldPassword.text?.clear()
                                        edtNewPassword.text?.clear()
                                        edtConfirmPassword.text?.clear()
                                    }
                                } else {
                                    showToastMessage(getString(R.string.msg_password_change_failed))
                                }
                            }
                    } else {
                        showToastMessage(getString(R.string.msg_old_password_invalid))
                    }
                }
        }
    }

    private fun initToolbar() {
        val imgBack = findViewById<ImageView>(R.id.img_toolbar_back)
        val tvTitle = findViewById<TextView>(R.id.tv_toolbar_title)

        tvTitle.text = getString(R.string.change_password)
        imgBack.setOnClickListener { finish() }
    }
}
