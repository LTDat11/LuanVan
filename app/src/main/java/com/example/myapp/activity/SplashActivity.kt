package com.example.myapp.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.myapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private val db: FirebaseFirestore = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Tạo kênh thông báo
        createNotificationChannel()

        lifecycleScope.launch {
            delay(2000) // Delay 2 seconds
            if (isNetworkAvailable()) {
                checkUserLoginStatus()
            } else {
                showNoInternetDialog()
            }
        }

    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Kênh mặc định
            val channelId = "default_channel_id"
            val channelName = "Default Channel"
            val channelDescription = "Channel for default notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            // Kênh ưu tiên cao
            val highPriorityChannelId = "high_priority_channel_id"
            val highPriorityChannelName = "High Priority Channel"
            val highPriorityChannelDescription = "Channel for high priority notifications"
            val highPriorityImportance = NotificationManager.IMPORTANCE_HIGH

            val highPriorityChannel = NotificationChannel(highPriorityChannelId, highPriorityChannelName, highPriorityImportance).apply {
                description = highPriorityChannelDescription
            }

            // Kênh ưu tiên thấp
            val lowPriorityChannelId = "low_priority_channel_id"
            val lowPriorityChannelName = "Low Priority Channel"
            val lowPriorityChannelDescription = "Channel for low priority notifications"
            val lowPriorityImportance = NotificationManager.IMPORTANCE_LOW

            val lowPriorityChannel = NotificationChannel(lowPriorityChannelId, lowPriorityChannelName, lowPriorityImportance).apply {
                description = lowPriorityChannelDescription
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(highPriorityChannel)
            notificationManager.createNotificationChannel(lowPriorityChannel)
        }
    }

    private fun checkUserLoginStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val userDocRef = db.collection("Users").document(uid)
            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        //Kiểm tra role của user để chuyển hướng
                        val role = document.getString("role")
                        if (role == "Customer") {
                            goToMainActivity()
                        }else if (role == "Technician") {
                            goToTechnicianActivity()
                        }
                        else {
                            goToAdminActivity()
                        }
                    } else {
                        Toast.makeText(this, "document có vấn đề", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "lỗi", Toast.LENGTH_SHORT).show()
                }
        } else {
            goToLoginActivity()
        }
    }

    private fun goToTechnicianActivity() {
        val intent = Intent(this, TechnicianActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToLoginActivity() {
        intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToMainActivity() {
        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToAdminActivity() {
        val intent = Intent(this, AdminActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Không có kết nối mạng")
            .setMessage("Vui lòng kiểm tra kết nối mạng và thử lại.")
            .setPositiveButton("Thử lại") { _, _ -> recreate() }
            .setNegativeButton("Thoát") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}