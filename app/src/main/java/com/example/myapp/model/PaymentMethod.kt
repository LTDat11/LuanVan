package com.example.myapp.model

data class PaymentMethod(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    var isAvailable: Boolean = true
)
