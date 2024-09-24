package com.example.myapp.fragment

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.activity.ChangePassActivity
import com.example.myapp.activity.ContactActivity
import com.example.myapp.activity.LoginActivity
import com.example.myapp.activity.MainActivity
import com.example.myapp.activity.SendFeedbackActivity
import com.example.myapp.activity.SplashActivity
import com.example.myapp.activity.UserInfoActivity
import com.example.myapp.activity.ViewFeedbackActivity
import com.example.myapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
        // Đăng xuất
        val layoutSignOut = mView?.findViewById<View>(R.id.layout_sign_out)
        layoutSignOut?.setOnClickListener { confirmSignOut() }

        //Sửa thông tin cá nhân
        val layoutEditProfile = mView?.findViewById<View>(R.id.layout_user_info)
        layoutEditProfile?.setOnClickListener {
            val intent = Intent(activity, UserInfoActivity::class.java)
            startActivity(intent)
        }

        //Đổi mật khẩu
        val layoutChangePass = mView?.findViewById<View>(R.id.layout_change_password)
        layoutChangePass?.setOnClickListener {
            val intent = Intent(activity, ChangePassActivity::class.java)
            startActivity(intent)
        }

        val layoutContact = mView?.findViewById<View>(R.id.layout_contact)
        layoutContact?.setOnClickListener {
            val intent = Intent(activity, ContactActivity::class.java)
            startActivity(intent)
        }
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

        // Tạo Intent để khởi động lại ứng dụng
        val restartIntent = Intent(activity, SplashActivity::class.java)
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        // Tạo PendingIntent để restart app
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Sử dụng AlarmManager để restart ứng dụng ngay lập tức
        val alarmManager = activity?.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent
        )

        // Đóng tất cả activity
        activity?.finishAffinity()
    }


    private fun initUi() {
        val tvEmail = mView?.findViewById<TextView>(R.id.tv_email)
        val tvName = mView?.findViewById<TextView>(R.id.tv_user_name)
        val civ_avatar = mView?.findViewById<ImageView>(R.id.civ_avatar)
        val layoutFeedback = mView?.findViewById<View>(R.id.layout_feedback)
        val tvFeedback = mView?.findViewById<TextView>(R.id.tv_feedback)

        CoroutineScope(Dispatchers.Main).launch {
            val userInfo = getUserInfo()
            if (userInfo != null) {
                tvName?.text = userInfo.name ?: getString(R.string.default_name)
                tvEmail?.text = userInfo.email ?: firebaseAuth.currentUser?.email

                // Load ảnh đại diện
                if (userInfo.imageURL != null) {
                    Glide.with(this@AccountFragment)
                        .load(userInfo.imageURL)
                        .circleCrop()
                        .into(civ_avatar!!)
                }

                // Kiểm tra vai trò của người dùng
                if (userInfo.role == "Admin") {
                    tvFeedback?.text = getString(R.string.view_feedback)
                    layoutFeedback?.setOnClickListener {
                        val intent = Intent(context, ViewFeedbackActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    tvFeedback?.text = getString(R.string.send_feedback)
                    layoutFeedback?.setOnClickListener {
                        val intent = Intent(context, SendFeedbackActivity::class.java)
                        intent.putExtra("name", userInfo.name)
                        intent.putExtra("email", userInfo.email)
                        intent.putExtra("phone", userInfo.phone)
                        startActivity(intent)
                    }
                }
            }
        }
    }



    // Hàm lấy thông tin người dùng từ Firestore theo thời gian thực, bao gồm cả role
    private suspend fun getUserInfo(): User? {
        val currentUser = firebaseAuth.currentUser ?: return null
        val uid = currentUser.uid

        return try {
            val documentSnapshot = firestore.collection("Users").document(uid).get().await()
            if (documentSnapshot.exists()) {
                documentSnapshot.toObject(User::class.java)?.apply {
                    id = uid  // Đặt ID từ currentUser vào object
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }



    // set up the toolbar text, back button click listener and set text to Account
    private fun initToolbar() {
//        val imgToolbarBack = mView?.findViewById<ImageView>(R.id.img_toolbar_back)
        val tvToolbarTitle = mView?.findViewById<TextView>(R.id.tv_toolbar_title)
//        imgToolbarBack?.setOnClickListener { backToHomeScreen(HomeFragment()) }
        tvToolbarTitle?.text = getString(R.string.nav_account)
    }

    private fun backToHomeScreen(fragment: Fragment) {
        val mainActivity = activity as MainActivity? ?: return
        mainActivity.viewPager2?.currentItem = 0
    }
}
