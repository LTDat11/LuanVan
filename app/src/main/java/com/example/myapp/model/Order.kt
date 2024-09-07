package com.example.myapp.model

import java.io.Serializable

data class Order(
    var id: String? = null,
    var id_customer: String? = null,
    var id_technician: String? = null,
    var id_servicepackage: String? = null,
    var status: String? = null,
    val createdAt: java.util.Date? = null,
    val updatedAt: java.util.Date? = null,
    var description: String? = null,
    var notes2: String? = null,
    var selectedBrand: String? = null,
    var imgURLServicePackage: String? = null,
    var namePackage: String? = null,
    var price: String? = null,
) : Serializable
