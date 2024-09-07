package com.example.myapp.model

data class Bill(
    var id: String? = null,
    var id_customer: String? = null,
    var id_technician: String? = null,
    var id_order: String? = null,
    var id_paymentMethod: String? = null,
    var total: String? = null,
    var createdAt: java.util.Date? = null
)
