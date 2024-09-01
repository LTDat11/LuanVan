package com.example.myapp.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.myapp.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CompleteProfileActivity : BaseActivity() {
    private var edtName: EditText? = null
    private var edtPhone: EditText? = null
    private var edtAddress: EditText? = null
    private var btnComplete: Button? = null
    private var isEnableButtonComplete = false
    private val db: FirebaseFirestore = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_profile)

        initUi()
        initListener()
        updateUIState()
    }

    private fun initListener() {
        edtName?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                updateUIState()
            }
        })
        edtPhone?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                updateUIState()
            }
        })
        edtAddress?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                updateUIState()
            }
        })
        btnComplete?.setOnClickListener { onClickValidateComplete() }
    }

    private fun onClickValidateComplete() {
        if (!isEnableButtonComplete) return
        // Thêm các trường dữ liệu vào Firestore tương ứng với user hiện tại và chuyển sang trang mainactivity
        val name = edtName?.text.toString().trim()
        val phone = edtPhone?.text.toString().trim()
        val address = edtAddress?.text.toString().trim()
        val user = Firebase.auth.currentUser
        val uid = user?.uid
        val userDocRef = db.collection("Users").document(uid!!)
        userDocRef.update(
            mapOf(
                "name" to name,
                "phone" to phone,
                "address" to address,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).addOnSuccessListener {
            // Thêm dữ liệu vào Firestore thành công
            Toast.makeText(this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            // Xử lý lỗi khi thêm dữ liệu vào Firestore
            Toast.makeText(this, "Lỗi khi cập nhật thông tin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIState() {
        val name = edtName?.text.toString()
        val phone = edtPhone?.text.toString()
        val address = edtAddress?.text.toString()

        val isNameValid = name.isNotEmpty()
        val isPhoneValid = phone.isNotEmpty()
        val isAddressValid = address.isNotEmpty()

        edtName?.setBackgroundResource(if (isNameValid) R.drawable.bg_white_corner_16_border_main else R.drawable.bg_white_corner_16_border_gray)
        edtPhone?.setBackgroundResource(if (isPhoneValid) R.drawable.bg_white_corner_16_border_main else R.drawable.bg_white_corner_16_border_gray)
        edtAddress?.setBackgroundResource(if (isAddressValid) R.drawable.bg_white_corner_16_border_main else R.drawable.bg_white_corner_16_border_gray)

        isEnableButtonComplete = isNameValid && isPhoneValid && isAddressValid
        // Cập nhật nút MaterialButton
        btnComplete?.apply {
            background = ContextCompat.getDrawable(
                context,
                if (isEnableButtonComplete) R.drawable.bg_button_enable_corner_16 else R.drawable.bg_button_disable_corner_16
            )
            isEnabled = isEnableButtonComplete
        }
    }

    private fun initUi() {
        edtName = findViewById(R.id.edt_name)
        edtPhone = findViewById(R.id.edt_phone)
        edtAddress = findViewById(R.id.edt_address)
        btnComplete = findViewById(R.id.btn_complete)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        showConfirmExitApp()
    }

    private fun showConfirmExitApp() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.confirm_exit_app))
        builder.setPositiveButton(getString(R.string.yes)) { _, _ ->
            finish()
        }
        builder.setNegativeButton(getString(R.string.no)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }
}