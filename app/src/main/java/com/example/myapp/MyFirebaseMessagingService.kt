package com.example.myapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapp.activity.MainActivity
import com.example.myapp.activity.SplashActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Xử lý thông báo nhận được ở đây
        remoteMessage.notification?.let {
            // Hiển thị thông báo
            showNotification(it.title, it.body)
        }
    }

    private fun showNotification(title: String?, body: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "my_channel_id"
        val channelName = "My Channel"

        // Tạo channel cho Android 8.0 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel description"
                // Thêm màu sắc cho channel
                //lightColor = Color.BLUE
                // Thêm âm thanh cho channel n
                //setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo Intent để mở ứng dụng khi nhấn vào thông báo
        val intent = Intent(this, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Tạo thông báo
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo) // Thay thế bằng icon của bạn
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true) // Tự động xóa thông báo khi nhấn
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Đặt mức độ ưu tiên cho thông báo

        // Hiển thị thông báo
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

}