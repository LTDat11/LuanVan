package com.example.myapp.activity

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp.R

abstract class BaseActivity: AppCompatActivity() {
    private var progressDialog: ProgressDialog? = null
    private var alertDialog: AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createProgressDialog()
        createAlertDialog()
    }

    private fun createProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.msg_waiting_message))
            isIndeterminate = true
            setCancelable(false)
        }
    }

    fun showProgressDialog(value: Boolean) {
        if (value) {
            progressDialog?.takeIf { !it.isShowing }?.show()
        } else {
            progressDialog?.takeIf { it.isShowing }?.dismiss()
        }
    }

    private fun createAlertDialog() {
        alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setPositiveButton(R.string.action_ok, null)
            .setCancelable(false)
            .create()
    }

    fun showAlertDialog(errorMessage: String?) {
        alertDialog?.setMessage(errorMessage)
        alertDialog?.show()
    }

    fun showAlertDialog(@StringRes resourceId: Int) {
        alertDialog?.setMessage(getString(resourceId))
        alertDialog?.show()
    }

    fun setCancelProgress(isCancel: Boolean) {
        progressDialog?.setCancelable(isCancel)
    }

    fun showToastMessage(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        progressDialog?.takeIf { it.isShowing }?.dismiss()
        alertDialog?.takeIf { it.isShowing }?.dismiss()
        super.onDestroy()
    }


}