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
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.myapp.R
import com.example.myapp.model.ApiResponse
import com.example.myapp.model.RetrofitInstance
import com.example.myapp.model.UserRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

//    private fun checkUserLoginStatus() {
//        val user = FirebaseAuth.getInstance().currentUser
//        if (user != null) {
//            val uid = user.uid
//            // Kiểm tra trạng thái người dùng
//            checkUserStatus(uid)
//
//            // Tiếp tục lấy thông tin người dùng nếu không bị khóa
//            val userDocRef = db.collection("Users").document(uid)
//            userDocRef.get()
//                .addOnSuccessListener { document ->
//                    if (document != null) {
//                        //Kiểm tra role của user để chuyển hướng
//                        val role = document.getString("role")
//                        if (role == "Customer") {
//                            goToMainActivity()
//                        }else if (role == "Technician") {
//                            goToTechnicianActivity()
//                        }
//                        else {
//                            goToAdminActivity()
//                        }
//                    } else {
//                        Toast.makeText(this, "document có vấn đề", Toast.LENGTH_SHORT).show()
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Toast.makeText(this, "lỗi", Toast.LENGTH_SHORT).show()
//                }
//        } else {
//            goToLoginActivity()
//        }
//    }

    private fun checkUserLoginStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            // Kiểm tra trạng thái người dùng từ server
            checkUserStatusFromServer(uid)
        } else {
            goToLoginActivity()
        }
    }

    private fun checkUserStatusFromServer(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {

            val request = UserRequest(uid)

            //thiết lập Retrofit để gọi API
            RetrofitInstance.api.checkUserStatus(request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val isDisabled = response.body()?.isDisabled ?: false
                        if (isDisabled) {
                            // Nếu người dùng bị khóa, thực hiện logout
                            FirebaseAuth.getInstance().signOut()
                            Toast.makeText(this@SplashActivity, "Tài khoản của bạn đã bị khóa.", Toast.LENGTH_LONG).show()
                            goToLoginActivity() // Quay lại trang đăng nhập
                        } else {
                            // Tiếp tục lấy thông tin người dùng
                            getUserInfo(uid)
                        }
                    } else if (response.code() == 404) {
                        // Nếu không tìm thấy người dùng, thực hiện logout
                        FirebaseAuth.getInstance().signOut()
                        Toast.makeText(this@SplashActivity, "Tài khoản của bạn đã bị xóa.", Toast.LENGTH_LONG).show()
                        goToLoginActivity() // Quay lại trang đăng nhập
                    }
                    else {
                        Log.d("checkUserStatus", "Lỗi: ${response.errorBody()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.d("checkUserStatus", "Lỗi: ${t.message}")
                }
            })

        }
    }

    private fun getUserInfo(uid: String) {
        val userDocRef = db.collection("Users").document(uid)
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Kiểm tra role của user để chuyển hướng
                    val role = document.getString("role")

                    when (role) {
                        "Customer" -> goToMainActivity()
                        "Technician" -> goToTechnicianActivity()
                        "Admin" -> goToAdminActivity()
                        else -> Toast.makeText(this, "Vai trò không hợp lệ.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Tài khoản không tồn tại.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi lấy thông tin người dùng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Hàm kiểm tra trạng thái người dùng

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