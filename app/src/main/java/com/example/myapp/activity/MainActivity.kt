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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
        // Kiểm tra vai trò người dùng và bắt đầu lắng nghe nếu là khách hàng
        checkUserRoleAndListenForCustomerUpdates()
    }

    private fun checkUserRoleAndListenForCustomerUpdates() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // Kiểm tra vai trò người dùng từ Firestore
        db.collection("Users").document(currentUser.uid).get()
            .addOnSuccessListener { documentSnapshot ->
                val role = documentSnapshot.getString("role")
                if (role == "Customer") {
                    listenToCustomerOrderUpdates(currentUser.uid)
                } else {
                    Log.d("ListenChange", "User is not a customer, no order updates will be listened to")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ListenChangeError", "Error fetching user role", e)
            }
    }

    private fun listenToCustomerOrderUpdates(customerId: String) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        // Danh sách trạng thái cần lọc
        val statuses = listOf("pending", "processing", "completed")

        db.collection("orders")
            .whereEqualTo("id_customer", customerId)
            .whereIn("status", statuses)  // Lọc đơn hàng theo trạng thái
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Xử lý lỗi nếu cần
                    Log.e("OrderUpdateError", "Error listening to order changes", e)
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

                    // Cập nhật badge và gửi thông báo nếu có đơn hàng hoàn tất
                    updateBadgeForBottomNav(totalCount)

                    if (hasCompletedOrder) {
                        sendNotification(
                            "high_priority_channel_id",
                            "Đơn hàng hoàn tất sửa chữa",
                            "Bạn có đơn hàng đã sửa chữa xong. Vui lòng kiểm tra để thanh toán!"
                        )
                    }
                }
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

        // Xóa thông báo cũ trước khi gửi thông báo mới
        notificationManager.cancel(channelId.hashCode()) // Xóa thông báo cũ, nếu có

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
