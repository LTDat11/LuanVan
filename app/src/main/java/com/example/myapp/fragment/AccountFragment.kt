package com.example.myapp.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.myapp.R
import com.example.myapp.activity.LoginActivity
import com.example.myapp.activity.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountFragment : Fragment() {
    private var mView: View? = null
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_account, container, false)
        initToolbar()
        initUi()
        initListener()
        return mView
    }
    private fun initListener() {
        val layoutSignOut = mView?.findViewById<View>(R.id.layout_sign_out)
        layoutSignOut?.setOnClickListener { confirmSignOut() }
    }

    private fun confirmSignOut() {
        val builder = context?.let { androidx.appcompat.app.AlertDialog.Builder(it) }
        builder?.setTitle(getString(R.string.confirm_sign_out))
        builder?.setPositiveButton(getString(R.string.yes)) { _, _ -> signOut() }
        builder?.setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
        builder?.show()
    }

    //    log out of the app
    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(activity, LoginActivity::class.java)

        // Clear the activity stack to prevent the user from going back to this activity after signing out
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Start LoginActivity
        startActivity(intent)
    }

    private fun initUi() {
        val tvEmail = mView?.findViewById<TextView>(R.id.tv_email)
        val tvName = mView?.findViewById<TextView>(R.id.tv_user_name)

        // Gọi hàm lấy thông tin người dùng từ Firestore
        getUserInfo { name, email ->
            tvName?.text = name ?: getString(R.string.default_name)
            tvEmail?.text = email ?: firebaseAuth.currentUser?.email
        }

    }

    // Hàm lấy thông tin người dùng từ Firestore
    private fun getUserInfo(callback: (name: String?, email: String?) -> Unit) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            firestore.collection("Users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val email = document.getString("email")
                        callback(name, email)  // Trả kết quả về thông qua callback
                    } else {
                        callback(null, currentUser.email)  // Trả email nếu không tìm thấy document
                    }
                }
                .addOnFailureListener {
                    callback(null, currentUser.email)  // Trả email nếu có lỗi
                }
        } else {
            callback(null, null)  // Nếu không có người dùng đăng nhập, trả về null
        }
    }

// set up the toolbar text, back button click listener and set text to Account
    private fun initToolbar() {
        val imgToolbarBack = mView?.findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = mView?.findViewById<TextView>(R.id.tv_toolbar_title)
        imgToolbarBack?.setOnClickListener { backToHomeScreen(HomeFragment()) }
        tvToolbarTitle?.text = getString(R.string.nav_account)
    }

    private fun backToHomeScreen(fragment: Fragment) {
        val mainActivity = activity as MainActivity? ?: return
        mainActivity.viewPager2?.currentItem = 0
    }
}
