package com.example.myapp.activity

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.MyViewPagerTechnicainAdapter
import com.example.myapp.databinding.ActivityTechnicianBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class TechnicianActivity : AppCompatActivity() {
    lateinit var binding: ActivityTechnicianBinding
    private var mBottomNavigationView: BottomNavigationView? = null
    var viewPager2: ViewPager2? = null
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var previousCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTechnicianBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kiểm tra thông tin người dùng và chuyển hướng tới trang điền thông tin còn thiếu
        checkUserProfileAndNavigate()
        listenChange()

        binding.apply {
            mBottomNavigationView = bottomNavigation
            viewPager2 = viewpager2
            viewPager2?.isUserInputEnabled = false

            val myViewPagerTechnincainAdapter = MyViewPagerTechnicainAdapter(this@TechnicianActivity)
            viewPager2?.adapter = myViewPagerTechnincainAdapter

            viewpager2?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    when (position) {
                        0 -> mBottomNavigationView?.menu?.findItem(R.id.nav_job)?.isChecked = true
                        1 -> mBottomNavigationView?.menu?.findItem(R.id.nav_account)?.isChecked = true
                    }
                }
            })
            mBottomNavigationView?.setOnNavigationItemSelectedListener{item ->
                when (item.itemId) {
                    R.id.nav_job -> {
                        viewPager2?.currentItem = 0
                    }
                    R.id.nav_account -> {
                        viewPager2?.currentItem = 1
                    }
                }
                true
            }
        }

    }

    private fun listenChange() {
        CoroutineScope(Dispatchers.IO).launch {
            val isTechnician = checkUserRole()

            if (isTechnician) {
                val db = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()

                withContext(Dispatchers.Main) {
                    db.collection("orders")
                        .whereEqualTo("id_technician", auth.currentUser?.uid)
                        .whereEqualTo("status", "processing")
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                // Xử lý lỗi nếu cần
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val count = snapshot.size()
                                if (count > previousCount) {
                                    sendNotification(
                                        count,
                                        "high_priority_channel_id",
                                        "Đơn hàng được phân công",
                                        "Bạn có $count đơn hàng được phân công."
                                    )
                                }

                                // Cập nhật giá trị previousCount
                                previousCount = count
                            }
                        }
                }
            }
        }
    }

    // Hàm kiểm tra vai trò người dùng
    private suspend fun checkUserRole(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()

        return try {
            val documentSnapshot = db.collection("Users").document(currentUser.uid).get().await()
            val role = documentSnapshot.getString("role")
            role == "Technician"  // Chỉ cho phép kỹ thuật viên lắng nghe đơn hàng
        } catch (e: Exception) {
            false
        }
    }


    private fun sendNotification(count: Int, channelId: String, title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.baseline_fiber_new_24)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(channelId.hashCode(), notificationBuilder.build())
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

    private fun checkUserProfileAndNavigate() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        // Check user profile status from Firestore
        val docRef = db.collection("Users").document(userId)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val phone = document.getString("phone")
                    val address = document.getString("address")

                    if (name.isNullOrEmpty() || phone.isNullOrEmpty() || address.isNullOrEmpty()) {
                        navigateToCompleteProfile()
                    }
                } else {

                }
            }
            .addOnFailureListener {
                // Handle failure (e.g., show a message to the user)
            }
    }

    private fun navigateToCompleteProfile() {
        val intent = Intent(this, CompleteProfileActivity::class.java)
        startActivity(intent)
    }
}