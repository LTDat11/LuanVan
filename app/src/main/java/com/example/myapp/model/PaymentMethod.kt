package com.example.myapp.model

data class PaymentMethod(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val isAvailable: Boolean = false
)
