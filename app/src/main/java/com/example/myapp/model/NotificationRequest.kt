package com.example.myapp.model

data class NotificationRequest(
    val token: String,
    val title: String,
    val body: String
)
