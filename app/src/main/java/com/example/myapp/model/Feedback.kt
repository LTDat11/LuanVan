package com.example.myapp.model

data class Feedback(
    var name: String? = null,
    var phone: String? = null,
    var email: String? = null,
    var comment: String? = null,
    var rating: Float? = null,
    val createdAt: java.util.Date? = null
)