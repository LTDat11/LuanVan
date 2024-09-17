package com.example.myapp.activity

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.MyViewPagerAdapter
import com.example.myapp.databinding.ActivityMainBinding
import com.example.myapp.fragment.AccountFragment
import com.example.myapp.fragment.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var mBottomNavigationView: BottomNavigationView? = null
    var viewPager2: ViewPager2? = null
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kiểm tra thông tin người dùng và chuyển hướng tới trang điền thông tin còn thiếu
        checkUserProfileAndNavigate()
        listenChange()

        binding.apply {
            mBottomNavigationView = bottomNavigation
            viewPager2 = viewpager2
            viewPager2?.isUserInputEnabled = false
            val myViewPagerAdapter = MyViewPagerAdapter(this@MainActivity)
            viewPager2?.adapter = myViewPagerAdapter
            viewPager2?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    when (position) {
                        0 -> mBottomNavigationView?.menu?.findItem(R.id.nav_home)?.isChecked = true
                        1 -> mBottomNavigationView?.menu?.findItem(R.id.nav_history)?.isChecked = true
                        2 -> mBottomNavigationView?.menu?.findItem(R.id.nav_account)?.isChecked = true
                    }
                }
            })
            mBottomNavigationView?.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        viewPager2?.currentItem = 0
                    }
                    R.id.nav_history -> {
                        viewPager2?.currentItem = 1
                    }
                    R.id.nav_account -> {
                        viewPager2?.currentItem = 2
                    }
                }
                true
            }

        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        showConfirmExitApp()
    }

    private fun listenChange() {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        // Kiểm tra vai trò người dùng trước khi tiếp tục
        checkUserRole { isCustomer ->
            if (isCustomer) {
                // Danh sách trạng thái cần lọc
                val statuses = listOf("pending", "processing", "completed")

                // Lắng nghe thay đổi của đơn hàng theo các trạng thái
                db.collection("orders")
                    .whereEqualTo("id_customer", auth.currentUser?.uid)
                    .whereIn("status", statuses) // Dùng whereIn để lọc các trạng thái
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            // Xử lý lỗi nếu cần
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            // Đếm số lượng đơn hàng theo trạng thái
                            var totalCount = 0
                            var hasCompletedOrder = false

                            for (doc in snapshot.documents) {
                                val status = doc.getString("status") ?: continue
                                if (statuses.contains(status)) {
                                    totalCount++
                                }
                                if (status == "completed") {
                                    hasCompletedOrder = true
                                }
                            }

                            updateBadgeForBottomNav(totalCount)

                            // Kiểm tra nếu có đơn hàng hoàn thành và gửi thông báo
                            if (hasCompletedOrder) {
                                sendNotification("high_priority_channel_id", "Đơn hàng hoàn tất sửa chữa", "Bạn có đơn hàng đã sửa chữa xong. Vui lòng kiểm tra để thanh toán!.")
                            }
                        }
                    }
            }
        }
    }

    // Hàm kiểm tra vai trò người dùng
    private fun checkUserRole(callback: (Boolean) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("Users").document(currentUser.uid).get().addOnSuccessListener { documentSnapshot ->
                val role = documentSnapshot.getString("role")
                // Kiểm tra nếu vai trò là customer (khách hàng)
                callback(role == "Customer")
            }.addOnFailureListener {
                // Xử lý lỗi nếu không lấy được role
                callback(false)
            }
        } else {
            callback(false)
        }
    }

    private fun updateBadgeForBottomNav(totalCount: Int) {
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavView?.getOrCreateBadge(R.id.nav_history)?.apply {
            isVisible = totalCount > 0
            number = totalCount
        }
    }

    private fun sendNotification(channelId: String, title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_step_enable)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(channelId.hashCode(), notificationBuilder.build())
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
