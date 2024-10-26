package com.example.myapp.activity

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.MyViewPagerAdminAdapter
import com.example.myapp.databinding.ActivityAdminBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AdminActivity : AppCompatActivity() {
    lateinit var binding: ActivityAdminBinding
    private var mBottomNavigationView: BottomNavigationView? = null
    var viewPager2: ViewPager2? = null
    private var previousCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kiểm tra thông tin người dùng và chuyển hướng tới trang điền thông tin còn thiếu
        checkUserProfileAndNavigate()

        binding.apply {
            mBottomNavigationView = bottomNavigation
            viewPager2 = viewpager2
            viewPager2?.isUserInputEnabled = false
            val myViewPagerAdminAdapter = MyViewPagerAdminAdapter(this@AdminActivity)
            viewpager2?.adapter = myViewPagerAdminAdapter
            viewPager2?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    when (position) {
                        0 -> mBottomNavigationView?.menu?.findItem(R.id.nav_home)?.isChecked = true
                        1 -> mBottomNavigationView?.menu?.findItem(R.id.nav_list)?.isChecked = true
//                        2 -> mBottomNavigationView?.menu?.findItem(R.id.nav_more)?.isChecked = true
                        2 -> mBottomNavigationView?.menu?.findItem(R.id.nav_category)?.isChecked = true
                        3 -> mBottomNavigationView?.menu?.findItem(R.id.nav_account)?.isChecked = true
                    }
                }
            })
            mBottomNavigationView?.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        viewPager2?.currentItem = 0
                    }
                    R.id.nav_list -> {
                        viewPager2?.currentItem = 1
                    }
                    R.id.nav_more -> {
//                        viewPager2?.currentItem = 2
                        showBottomSheetDialog()
                    }
                    R.id.nav_category -> {
                        viewPager2?.currentItem = 2
                    }
                    R.id.nav_account -> {
                        viewPager2?.currentItem = 3
                    }
                }
                true
            }
        }
        listenChange()
    }

    private fun checkUserProfileAndNavigate() {
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return
        val userId = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
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
        }

    }

    private fun navigateToCompleteProfile() {
        val intent = Intent(this, CompleteProfileActivity::class.java)
        startActivity(intent)
    }


    private fun listenChange() {
        CoroutineScope(Dispatchers.IO).launch {
//            val isAdmin = checkUserRole()

            withContext(Dispatchers.Main) {
                val db = FirebaseFirestore.getInstance()
                db.collection("orders")
                    .whereEqualTo("status", "pending")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val count = snapshot.size()

//                            if (isAdmin && count > previousCount) {
//                                sendNotification(
//                                    count,
//                                    "high_priority_channel_id",
//                                    "Đơn hàng mới",
//                                    "Bạn có $count đơn hàng mới đang chờ xác nhận."
//                                )
//                            }

                            updateBadgeForBottomNav(count)
                            previousCount = count
                        }
                    }
            }
        }
    }


    private suspend fun checkUserRole(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()

        return try {
            val documentSnapshot = db.collection("Users").document(currentUser.uid).get().await()
            val role = documentSnapshot.getString("role")
            role == "Admin" // Chỉ cho phép admin nhận thông báo
        } catch (e: Exception) {
            false
        }
    }

    private fun updateBadgeForBottomNav(count: Int) {
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavView?.getOrCreateBadge(R.id.nav_list)?.apply {
            isVisible = count > 0
            number = count
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

        // Xóa thông báo cũ trước khi gửi thông báo mới
        notificationManager.cancel(channelId.hashCode()) // Xóa thông báo cũ, nếu có

        notificationManager.notify(channelId.hashCode(), notificationBuilder.build())
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetView.findViewById<TextView>(R.id.option_one).setOnClickListener {
            val intent = Intent(this, TechManagementActivity::class.java)
            startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<TextView>(R.id.option_two).setOnClickListener {
            val intent = Intent(this, CustomerManagementActivity::class.java)
            startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<TextView>(R.id.option_three).setOnClickListener {
            val intent = Intent(this, BannerManagementActivity::class.java)
            startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<TextView>(R.id.option_four).setOnClickListener {
            val intent = Intent(this, AdminManagementActivity::class.java)
            startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<TextView>(R.id.option_five).setOnClickListener {
            val intent = Intent(this, PaymentMethodManagementActivity::class.java)
            startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setOnDismissListener {
            // Chuyển menu về trang home
            mBottomNavigationView?.menu?.findItem(R.id.nav_home)?.isChecked = true
            viewPager2?.currentItem = 0
        }

        bottomSheetDialog.setOnDismissListener {
            // Chuyển menu về trang home
            mBottomNavigationView?.menu?.findItem(R.id.nav_home)?.isChecked = true
            viewPager2?.currentItem = 0
        }

        bottomSheetDialog.show()
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