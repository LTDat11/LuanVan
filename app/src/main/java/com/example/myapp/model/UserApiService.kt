package com.example.myapp.model

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface UserApiService {
    @POST("/disable-user")
    fun disableUser(@Body request: UserRequest): Call<ApiResponse>

    @POST("/enable-user")
    fun enableUser(@Body request: UserRequest): Call<ApiResponse>

    @DELETE("/delete-user/{uid}")
    fun deleteUser(@Path("uid") uid: String): Call<ApiResponse>

    @POST("/check-user-status")
    fun checkUserStatus(@Body request: UserRequest): Call<ApiResponse>

    @POST("/set-custom-claims")
    fun setCustomClaims(@Body request: UserRequest): Call<ApiResponse>

}