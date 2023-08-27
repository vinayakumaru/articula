package com.example.articula

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.example.articula.databinding.ActivityMainBinding
import com.example.articula.model.EditResponse
import com.example.articula.model.Embedding
import com.example.articula.network.ApiCallback
import com.example.articula.network.ApiClient
import com.example.articula.samOnnx.OnnxModel
import com.example.articula.utils.editImage.ProcessMask
import com.example.articula.utils.message.ShowMessage
import com.example.articula.utils.imageStack.ImageStack
import com.example.articula.utils.imageIO.ImageFromUri
import com.example.articula.utils.imageIO.SaveEmbeddings
import com.example.articula.utils.permission.Permission
import com.example.articula.utils.scaledCoordinates.ScaledCoordinates
import com.example.articula.utils.speech.RecognizeSpeech
import com.example.articula.utils.speech.RecognizeSpeechListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var recognizeSpeech: RecognizeSpeech
    private val imageStack = ImageStack()
    private val apiClient = ApiClient(this,BuildConfig.BASE_URL)
    private val onnxModel : OnnxModel by lazy { OnnxModel(this) }
    private var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecognizeSpeech()
        setupAddImageButton()
        setupImageView()
    }


    private fun setupRecognizeSpeech(){
        recognizeSpeech = RecognizeSpeech(this)
        recognizeSpeech.setListener(object : RecognizeSpeechListener {
            override fun onListeningStarted() {
                binding.micButton.setImageDrawable(AppCompatResources.getDrawable(this@MainActivity, R.drawable.round_mic_24))
            }

            override fun onListeningStopped() {
                binding.micButton.setImageDrawable(AppCompatResources.getDrawable(this@MainActivity, R.drawable.round_mic_off_24))
            }

            override fun onResult(result: String) {
                sendCommandToServer(result)
            }
        })
        binding.micButton.setOnClickListener {
            recognizeSpeech.clicked()
        }
    }


    private fun setupAddImageButton(){
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let{ it1 ->
                val bitmap = ImageFromUri(this).getBitmapFromUri(it1)
                bitmap?.let {
                    imageStack.clear()
                    imageStack.clearMask()
                    imageStack.push(it)
                    binding.imageView.setImageBitmap(it)
                    CoroutineScope(Dispatchers.IO).launch {
                        loadEmbeddings(it1,it)
                    }
                }
            }
        }
        binding.add.setOnClickListener {
            if(Permission(this).isPermissionsGranted(Permission.getImagePermissions())){
                getContent.launch("image/*")
            }
            else{
                Permission(this).requestPermissions(Permission.getImagePermissions(), object : Permission.PermissionListener {
                    override fun onPermissionsGranted() {
                        getContent.launch("image/*")
                    }
                    override fun onPermissionsDenied(deniedPermissions: List<String>) {
                        ShowMessage.show(this@MainActivity,"Permission denied  to read external storage")
                    }
                })
            }
        }
    }

    private fun loadEmbeddings(uri: Uri,bitmap: Bitmap) {
        val filename = "${ImageFromUri(this).getFileNameFromUri(uri)!!}.bin"
        val byteArray = SaveEmbeddings.getEmbeddings(this,filename)
        if (byteArray != null) {
            setEmbeddings(byteArray)
        }
        else{
            apiClient.getImageEmbeddings(bitmap,object : ApiCallback<ByteArray>{
                override fun onResult(t: ByteArray) {
                    setEmbeddings(t)
                    SaveEmbeddings.saveEmbeddings(this@MainActivity,t,filename)
                }
            })
        }
    }

    private fun setEmbeddings(byteArray: ByteArray){
        val embedding = Embedding.FloatArray.parseFrom(byteArray)
        val floatArray = embedding.dataList.toFloatArray()
        val shape = embedding.shapeList.map { it.toLong() }.toLongArray()
        onnxModel.setEmbeddingTensor(floatArray,shape)
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupImageView(){
        binding.imageView.setOnTouchListener { _, event ->
            if(imageStack.isEmpty()) return@setOnTouchListener false

            if (event.action == MotionEvent.ACTION_DOWN && !clicked) {
                clicked = true
                val scaledCoordinates = ScaledCoordinates(this.resources.configuration.orientation)

                scaledCoordinates.check(
                    event.x,event.y,
                    imageStack.peek().height,imageStack.peek().width,
                    binding.imageView.height,binding.imageView.width
                )

                if (!scaledCoordinates.getIsValid()) {
                    clicked = false
                    return@setOnTouchListener false
                }

                startRippleAnimation(event.x,event.y)

                CoroutineScope(Dispatchers.IO).launch {
                    predictMask(
                        scaledCoordinates.getX(),scaledCoordinates.getY(),
                        imageStack.peek().height,imageStack.peek().width
                    )
                    clicked = false
                }

                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
    }


    private fun predictMask(x: Float, y: Float, height: Int, width: Int){
        val mask = onnxModel.getMask(x,y,height,width)
        mask?.let {
            imageStack.setMaskBitmap(ProcessMask.getBinaryMask(it,width,height))
            val bitmap = ProcessMask.addMaskToImage(imageStack.peek(),imageStack.getMaskBitmap()!!)
            runOnUiThread {
                binding.imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun startRippleAnimation(eventX: Float, eventY: Float) {
        val rippleView: View = findViewById(R.id.rippleView)
        val rippleDrawable = rippleView.foreground as RippleDrawable
        rippleView.x = eventX - rippleView.width / 2
        rippleView.y = eventY - rippleView.height / 2
        rippleDrawable.state = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
        rippleDrawable.state = intArrayOf()
    }

    private fun sendCommandToServer(command: String){
        apiClient.sendCommand(command,object : ApiCallback<EditResponse>{
            override fun onResult(t: EditResponse) {

            }
        })
    }

}
