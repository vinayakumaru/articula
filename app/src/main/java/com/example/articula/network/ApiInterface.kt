package com.example.articula.network

import com.example.articula.model.EditResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiInterface {

    @Headers("Content-Type: application/json")
    @POST("/processCommand")
    fun sendCommand(@Body command: RequestBody): Call<EditResponse>

    @Multipart
    @POST("/inpaint")
    fun inpaintImage(
        @Part imageFile: MultipartBody.Part,
        @Part mask: MultipartBody.Part,
        @Part("json_data") jsonData: RequestBody
    ): Call<ResponseBody>

    @Multipart
    @POST("/embeddings")
    fun getEmbeddings(
        @Part imageFile: MultipartBody.Part,
    ): Call<ResponseBody>
}

