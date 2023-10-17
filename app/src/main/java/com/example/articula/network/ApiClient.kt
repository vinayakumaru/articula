package com.example.articula.network

import android.content.Context
import android.graphics.Bitmap
import com.example.articula.model.Command
import com.example.articula.model.EditResponse
import com.example.articula.utils.imageIO.SaveImageInCache
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context,baseUrl: String) {
    private val apiInterface: ApiInterface
    init {
        apiInterface = createApiService(baseUrl)
    }
    private fun createApiService(url : String): ApiInterface {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
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
                    else{
                        callback.onError("No result")
                    }
                }
                else{
                    callback.onError(response.message())
                }
            }
            override fun onFailure(call: Call<EditResponse>, t: Throwable) {
                println("Error: ${t.message}")
                callback.onError(t.message.toString())
            }
        })
    }

    fun getImageEmbeddings(bitmap: Bitmap,apiCallback : ApiCallback<ByteArray>){
        val file = SaveImageInCache.getImage(context,bitmap,"image.jpg")
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val imageFilePart = MultipartBody.Part.createFormData("image", file.name, requestBody)

        val call = apiInterface.getEmbeddings(imageFilePart)
        call.enqueue(object : Callback<ResponseBody>{
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if(response.isSuccessful){
                    val result = response.body()?.byteStream()?.use { it.readBytes() }
                    if (result != null) {
                        apiCallback.onResult(result)
                    }
                    else{
                        apiCallback.onError("No result")
                    }
                }
                else{
                    apiCallback.onError(response.message())
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("Error: ${t.message}")
                apiCallback.onError(t.message.toString())
            }
        })
    }
}