package com.example.articula.network

import com.example.articula.model.Command
import com.example.articula.model.EditResponse
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(baseUrl: String) {
    private val apiInterface: ApiInterface
    init {
        apiInterface = createApiService(baseUrl)
    }
    private fun createApiService(url : String): ApiInterface {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiInterface::class.java)
    }

    fun sendCommand(s: String, callback: ApiCallback<EditResponse>) {
        val command = Command(s)
        val json = Gson().toJson(command)
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
        val call = apiInterface.sendCommand(requestBody)
        call.enqueue(object : Callback<EditResponse>{
            override fun onResponse(call: Call<EditResponse>, response: Response<EditResponse>) {
                if (response.isSuccessful) {
                    val editResponse = response.body()
                    if (editResponse != null) {
                        callback.onResult(editResponse)
                    }
                }
            }
            override fun onFailure(call: Call<EditResponse>, t: Throwable) {
                println("Error: ${t.message}")
            }
        })
    }


}