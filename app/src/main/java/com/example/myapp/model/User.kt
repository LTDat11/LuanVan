package com.example.myapp.model

data class User(
    var id: String = "",
    var email: String = "",
    val createdAt: java.util.Date? = null,
    val updatedAt: java.util.Date? = null,
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val imageURL: String? = null,
    val role: String? = null,
    val description: String? = null
)
