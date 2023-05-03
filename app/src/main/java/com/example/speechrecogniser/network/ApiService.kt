package com.example.speechrecogniser.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import com.example.speechrecogniser.MainActivity.EditResponse

interface ApiService {
    @Multipart
    @POST("/edit")
    fun uploadImage(
        @Part imageFile: MultipartBody.Part,
        @Part("json_data") jsonData: RequestBody
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/getType")
    fun sendCommand(@Body command: RequestBody): Call<EditResponse>
}

