package com.example.speechrecogniser

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.example.speechrecogniser.constants.TypeOfOperation
import com.example.speechrecogniser.databinding.ActivityMainBinding
import com.example.speechrecogniser.network.ApiService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {
    companion object {
        private const val GALLERY_REQUEST_CODE = 0
        private const val BASE_URL = "http://voicevision.pythonanywhere.com/"
        private const val DELAY = 5000L
    }
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private var isListening = false
    private var tts: TextToSpeech? = null
    private var imageUri: Uri? = null
    private var typeOfEdit: Int = 0
    private var valueWithImage: Boolean = false
    private var apiService: ApiService? = null
    private lateinit var binding: ActivityMainBinding
    private val bitmapList = mutableListOf<Bitmap>()
    private var currentBitmap = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())


        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                binding.micButton.setImageDrawable(AppCompatResources.getDrawable(this@MainActivity, R.drawable.round_mic_24))
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                binding.micButton.setImageDrawable(AppCompatResources.getDrawable(this@MainActivity, R.drawable.round_mic_off_24))
            }

            override fun onError(error: Int) {
                isListening = false
                binding.micButton.setImageDrawable(AppCompatResources.getDrawable(this@MainActivity, R.drawable.round_mic_off_24))
                binding.micButton.isClickable = true
                valueWithImage = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    binding.resultTextView.text = matches[0]
                    if (!valueWithImage){
                        binding.micButton.isClickable = false
                        sendCommandToServer(matches[0])
                    }
                    else{
                        sendImageToServer(matches[0])
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        binding.micButton.setOnClickListener {
            if(imageUri == null){
                Snackbar.make(binding.root, "Please select an image first", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isListening) {
                speechRecognizer.startListening(speechRecognizerIntent)
            } else {
                speechRecognizer.stopListening()
                isListening = false
                binding.micButton.setImageDrawable(AppCompatResources.getDrawable(this@MainActivity, R.drawable.round_mic_off_24))
            }
        }

        binding.add.setOnClickListener {
            checkPermissionGranted()
        }

        tts = createTTSEngine()
        apiService = createApiService()

    }

    private fun createTTSEngine(): TextToSpeech {
        return TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts!!.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                } else {
                    // Set the voice to an Indian English accent
                    val voice = Voice("en-in-x-ena#female_1-local", Locale.ENGLISH, Voice.QUALITY_HIGH, Voice.LATENCY_LOW, false, null)
                    tts!!.voice = voice
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }
    }

    private fun createApiService(): ApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }

    private fun sendCommandToServer(s: String) {
        val command = Command(s)
        val json = Gson().toJson(command)
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
        val call = apiService?.sendCommand(requestBody)
        call?.enqueue(object : Callback<EditResponse> {
            override fun onResponse(call: Call<EditResponse>, response: Response<EditResponse>) {
                if(response.isSuccessful){
                    val editResponse = response.body()
                    if (editResponse != null) {
                        typeOfEdit = editResponse.typeOfEdit
                        val speakBack = editResponse.speakBack
                        val valueRequired = editResponse.valueRequired
                        valueWithImage = valueRequired
                        if(valueRequired){
                            speakOut(speakBack)
                            Handler.createAsync(Looper.getMainLooper()).postDelayed({
                                speechRecognizer.startListening(speechRecognizerIntent)
                            }, DELAY)
                        }
                        else if (typeOfEdit == TypeOfOperation.UNDO){
                            currentBitmap--
                            if (currentBitmap == -1){
                                currentBitmap = 0
                                speakOut("Nothing to undo")
                            } else {
                                speakOut(speakBack)
                                binding.imageView.setImageBitmap(bitmapList[currentBitmap])
                            }
                            binding.micButton.isClickable = true
                        }
                        else if (typeOfEdit == TypeOfOperation.REDO){
                            currentBitmap++
                            if (currentBitmap == bitmapList.size){
                                currentBitmap = bitmapList.size - 1
                                speakOut("Nothing to redo")
                            } else {
                                speakOut(speakBack)
                                binding.imageView.setImageBitmap(bitmapList[currentBitmap])
                            }
                            binding.micButton.isClickable = true
                        }
                        else{
                            speakOut(speakBack)
                            binding.micButton.isClickable = true
                        }
                    }
                } else {
                    binding.micButton.isClickable = true
                }
            }

            override fun onFailure(call: Call<EditResponse>, t: Throwable) {
                binding.micButton.isClickable = true
            }
        })

    }

    private fun speakOut(text : String){
        tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"")
    }

    private fun checkPermissionGranted() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            getPermission()
        } else {
            getGalleryImage()
        }
    }

    // function to get gallery image
    private fun getGalleryImage() {
        // Create an Intent to pick an image from the gallery
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    // function to get gallery and microphone permission
    private fun getPermission() {
        Dexter.withContext(this)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
            .withListener(object : MultiplePermissionsListener{
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if (p0!!.areAllPermissionsGranted()) {
                        getGalleryImage()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    showSnackbar()
                    p1!!.continuePermissionRequest()
                }

            }).onSameThread().check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            if (imageUri != null) {
                val bitmap = BitmapFactory.decodeFile(getPathFromUri(imageUri))
                bitmapList.clear()
                bitmapList.add(bitmap)
                binding.imageView.setImageBitmap(bitmapList[currentBitmap])
            }
        }
    }

    private fun showSnackbar() {
        Snackbar.make(binding.root, "Permission Denied", Snackbar.LENGTH_LONG).show()
    }

    private fun getPathFromUri(uri: Uri?): String? {
        if (uri == null) {
            return null
        }

        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            return it.getString(columnIndex)
        }

        return null
    }

    private fun sendImageToServer(s: String) {
        var value: Float
        try {
            val regex = "-?\\d+(?:\\.\\d+)?".toRegex()
            val matchResult = regex.find(s)
            val numbersOnly = matchResult?.value
            if(numbersOnly == null){
                speakOut("try with a valid number")
                Handler.createAsync(Looper.getMainLooper()).postDelayed({
                    speechRecognizer.startListening(speechRecognizerIntent)
                }, DELAY)
                return
            }
            value = numbersOnly.toFloat()
            if(s.contains("minus"))
                value *= -1

        } catch (e: NumberFormatException) {
            speakOut("try with a valid number")
            Handler.createAsync(Looper.getMainLooper()).postDelayed({
                speechRecognizer.startListening(speechRecognizerIntent)
            }, DELAY)
            return
        }
        val jsonData = JsonData(typeOfEdit, value)
        val file = getImageFromBitmap(bitmapList[currentBitmap])
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val imageFilePart = MultipartBody.Part.createFormData("image", file.name, requestBody)
        val jsonRequestBody = Gson().toJson(jsonData).toRequestBody("application/json".toMediaTypeOrNull())

        binding.progressBar.visibility = View.VISIBLE
        val call = apiService?.uploadImage(imageFilePart, jsonRequestBody)
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                binding.progressBar.visibility = View.GONE
                valueWithImage = false
                binding.micButton.isClickable = true
                if(response.isSuccessful){
                    val result = response.body()?.byteStream()
                    if (result != null) {
                        val bitmap = BitmapFactory.decodeStream(result)
                        bitmapList.add(bitmap)
                        currentBitmap++
                        binding.imageView.setImageBitmap(bitmapList[currentBitmap])
                    }
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.micButton.isClickable = true
                valueWithImage = false
            }
        })

    }

    private fun getImageFromBitmap(bitmap: Bitmap): File {
        val file = File(applicationContext.cacheDir, "image.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    data class JsonData(
        @SerializedName("type_of_edit") val typeOfEdit: Int,
        @SerializedName("val") val value: Float
    )

    data class Command(val cmd: String)

    data class EditResponse(
        val typeOfEdit: Int,
        val speakBack: String,
        val valueRequired: Boolean
    )
}


