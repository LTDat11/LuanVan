package com.example.myapp.model

import java.io.Serializable

data class ServicePackage(
    val id: String = "",
    val categoryId: String = "",
    val deviceId: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val price: String = "",
    val description: String = ""
): Serializable
