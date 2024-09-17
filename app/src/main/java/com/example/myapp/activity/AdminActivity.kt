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

    private fun listenChange() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){

                val db = FirebaseFirestore.getInstance()
                db.collection("orders")
                    .whereEqualTo("status", "pending")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val count = snapshot.size()

                            // Kiểm tra vai trò người dùng
                            checkUserRole { isAdmin ->
                                if (isAdmin && count > previousCount) {
                                    sendNotification(count, "high_priority_channel_id", "Đơn hàng mới", "Bạn có $count đơn hàng mới đang chờ xác nhận.")
                                }
                                updateBadgeForBottomNav(count)
                            }

                            previousCount = count
                        }

                    }
            }
        }
    }

    private fun checkUserRole(callback: (Boolean) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("Users").document(currentUser.uid).get().addOnSuccessListener { documentSnapshot ->
                val role = documentSnapshot.getString("role")
                callback(role == "admin") // Chỉ cho phép admin nhận thông báo
            }
        } else {
            callback(false)
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

        notificationManager.notify(channelId.hashCode(), notificationBuilder.build())
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetView.findViewById<TextView>(R.id.option_one).setOnClickListener {
//            val intent = Intent(this, OptionOneActivity::class.java)
//            startActivity(intent)
//            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Option One", Toast.LENGTH_SHORT).show()
        }

        bottomSheetView.findViewById<TextView>(R.id.option_two).setOnClickListener {
//            val intent = Intent(this, OptionTwoActivity::class.java)
//            startActivity(intent)
//            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Option Two", Toast.LENGTH_SHORT).show()
        }

        bottomSheetView.findViewById<TextView>(R.id.option_three).setOnClickListener {
            val intent = Intent(this, BannerManagementActivity::class.java)
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