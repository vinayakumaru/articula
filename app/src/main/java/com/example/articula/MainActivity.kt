package com.example.articula

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.ViewModelProvider
import com.example.articula.constants.TypeOfOperation
import com.example.articula.databinding.ActivityMainBinding
import com.example.articula.model.EditResponse
import com.example.articula.model.Embedding
import com.example.articula.network.ApiCallback
import com.example.articula.network.ApiClient
import com.example.articula.samOnnx.OnnxModel
import com.example.articula.utils.editImage.Operation
import com.example.articula.utils.editImage.ProcessMask
import com.example.articula.utils.imageIO.ImageFromUri
import com.example.articula.utils.imageIO.SaveEmbeddings
import com.example.articula.utils.imageIO.SaveImageToGallery
import com.example.articula.utils.imageStack.ImageStack
import com.example.articula.utils.message.ShowMessage
import com.example.articula.utils.permission.Permission
import com.example.articula.utils.scaledCoordinates.ScaledCoordinates
import com.example.articula.utils.speech.RecognizeSpeech
import com.example.articula.utils.speech.RecognizeSpeechListener
import com.example.articula.viewModel.MainActivityViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader


class MainActivity : AppCompatActivity() {

    private lateinit var mainActivityViewModel: MainActivityViewModel
    lateinit var binding: ActivityMainBinding
    private lateinit var recognizeSpeech: RecognizeSpeech
    private lateinit var imageStack: ImageStack
    private lateinit var onnxModel: OnnxModel
    private val apiClient = ApiClient(this, BuildConfig.BASE_URL)
    private var clicked = false
    private var typeOfOperation = TypeOfOperation.Invalid
    private var edited = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OpenCVLoader.initDebug()

        setupActivity()
        setupRecognizeSpeech()
        setupAddImageButton()
        setupImageView()
        setupBottomSheet()
        setupInfo()
        setupOperationButtons()
        setupToolBars()
    }



    private fun setupActivity() {
        mainActivityViewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]
        imageStack = mainActivityViewModel.imageStack
        onnxModel = mainActivityViewModel.onnxModel ?: OnnxModel(this).also {
            mainActivityViewModel.onnxModel = it
        }
    }

    private fun setupRecognizeSpeech() {
        recognizeSpeech = RecognizeSpeech(this)
        recognizeSpeech.setListener(object : RecognizeSpeechListener {
            override fun onListeningStarted() {
                binding.micButton.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this@MainActivity,
                        R.drawable.round_mic_24
                    )
                )
            }

            override fun onListeningStopped() {
                binding.micButton.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this@MainActivity,
                        R.drawable.round_mic_off_24
                    )
                )
            }

            override fun onResult(result: String) {
                sendCommandToServer(result)
            }
        })
        binding.micButton.setOnClickListener {
            recognizeSpeech.clicked()
        }
    }


    private fun setupAddImageButton() {
        val getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let { it1 ->
                    val bitmap = ImageFromUri(this).getBitmapFromUri(it1)
                    bitmap?.let {
                        imageStack.clear()
                        imageStack.push(it)
                        binding.imageView.setImageBitmap(it)
                        startImageAnimation()
                        onnxModel.closeEmbeddingTensor()
                        CoroutineScope(Dispatchers.IO).launch {
                            loadEmbeddings(it1, it)
                        }
                    }
                }
            }
        binding.add.setOnClickListener {
            closeBottomSheet()
            if (Permission(this).isPermissionsGranted(Permission.getImagePermissions())) {
                getContent.launch("image/*")
            } else {
                Permission(this).requestPermissions(
                    Permission.getImagePermissions(),
                    object : Permission.PermissionListener {
                        override fun onPermissionsGranted() {
                            getContent.launch("image/*")
                        }

                        override fun onPermissionsDenied(deniedPermissions: List<String>) {
                            ShowMessage.show(
                                this@MainActivity,
                                "Permission denied  to read external storage"
                            )
                        }
                    })
            }
        }
    }

    private fun startImageAnimation() {
        AnimationUtils.loadAnimation(this, R.anim.image_animation).also { anim ->
            binding.imageView.startAnimation(anim)
            binding.progressBar.visibility = View.VISIBLE
        }
    }

    private fun closeBottomSheet() {
        binding.bottomSheet.callOnClick()
    }

    private fun loadEmbeddings(uri: Uri, bitmap: Bitmap) {
        val filename = "${ImageFromUri(this).getFileNameFromUri(uri)!!}.bin"
        val byteArray = SaveEmbeddings.getEmbeddings(this, filename)
        if (byteArray != null) {
            setEmbeddings(byteArray)
            stopImageAnimation()
        } else {
            apiClient.getImageEmbeddings(bitmap, object : ApiCallback<ByteArray> {
                override fun onResult(t: ByteArray) {
                    setEmbeddings(t)
                    SaveEmbeddings.saveEmbeddings(this@MainActivity, t, filename)
                    stopImageAnimation()
                }

                override fun onError(message: String) {
                    stopImageAnimation()
                }
            })
        }
    }

    private fun stopImageAnimation() {
        runOnUiThread {
            binding.imageView.clearAnimation()
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun setEmbeddings(byteArray: ByteArray) {
        val embedding = Embedding.FloatArray.parseFrom(byteArray)
        val floatArray = embedding.dataList.toFloatArray()
        val shape = embedding.shapeList.map { it.toLong() }.toLongArray()
        onnxModel.setEmbeddingTensor(floatArray, shape)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupImageView() {
        if (!imageStack.isEmpty()) binding.imageView.setImageBitmap(imageStack.peek())
        binding.imageView.setOnTouchListener { _, event ->
            if (imageStack.isEmpty()) return@setOnTouchListener false

            if (event.action == MotionEvent.ACTION_DOWN && !clicked) {
                clicked = true
                val scaledCoordinates = ScaledCoordinates(this.resources.configuration.orientation)

                scaledCoordinates.check(
                    event.x, event.y,
                    imageStack.peek().height, imageStack.peek().width,
                    binding.imageView.height, binding.imageView.width
                )

                if (!scaledCoordinates.getIsValid()) {
                    clicked = false
                    return@setOnTouchListener false
                }

                startRippleAnimation(event.x, event.y)

                CoroutineScope(Dispatchers.IO).launch {
                    predictMask(
                        scaledCoordinates.getX(), scaledCoordinates.getY(),
                        imageStack.peek().height, imageStack.peek().width
                    )
                    clicked = false
                }

                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
    }


    private fun predictMask(x: Float, y: Float, height: Int, width: Int) {
        val mask = onnxModel.getMask(x, y, height, width)
        mask?.let {
            imageStack.setMaskBitmap(ProcessMask.getBinaryMask(it, width, height))
            handleEdit()
            val bitmap = ProcessMask.addMaskToImage(imageStack.peek(), imageStack.getMaskBitmap()!!)
            runOnUiThread {
                binding.imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun handleEdit() {
        if(edited){
            imageStack.push(binding.imageView.drawable.toBitmapOrNull()!!)
            edited = false
        }
    }

    private fun startRippleAnimation(eventX: Float, eventY: Float) {
        val rippleView: View = findViewById(R.id.rippleView)
        val rippleDrawable = rippleView.foreground as RippleDrawable
        rippleView.x = eventX - rippleView.width / 2
        rippleView.y = eventY - rippleView.height / 2
        rippleDrawable.state =
            intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
        rippleDrawable.state = intArrayOf()
    }

    private fun setupBottomSheet() {
        val sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        binding.bottomSheet.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun setupInfo() {
        binding.tvInfo.setOnClickListener {
            val transform = MaterialContainerTransform().apply {
                startView = binding.tvInfo
                endView = binding.cardViewInfo
                addTarget(endView)
                pathMotion = MaterialArcMotion()
                scrimColor = Color.TRANSPARENT
            }
            TransitionManager.beginDelayedTransition(binding.root, transform)
            binding.tvInfo.visibility = View.GONE
            binding.cardViewInfo.visibility = View.VISIBLE
        }
        binding.cardViewInfo.setOnClickListener {
            val transform = MaterialContainerTransform().apply {
                startView = binding.cardViewInfo
                endView = binding.tvInfo
                addTarget(endView)
                pathMotion = MaterialArcMotion()
                scrimColor = Color.TRANSPARENT
            }
            TransitionManager.beginDelayedTransition(binding.root, transform)
            binding.tvInfo.visibility = View.VISIBLE
            binding.cardViewInfo.visibility = View.GONE
        }
    }

    private fun setupOperationButtons() {
        binding.slider.addOnChangeListener { _, value, _ ->
            applyManipulation(value)
        }
        binding.brightnessButton.setOnClickListener {
            closeBottomSheet()
            showSlider()
            typeOfOperation = TypeOfOperation.BRIGHTNESS
        }
        binding.contrastButton.setOnClickListener {
            closeBottomSheet()
            showSlider()
            typeOfOperation = TypeOfOperation.CONTRAST
        }
        binding.saturationButton.setOnClickListener {
            closeBottomSheet()
            showSlider()
            typeOfOperation = TypeOfOperation.SATURATION
        }
        binding.hueButton.setOnClickListener {
            closeBottomSheet()
            showSlider()
            typeOfOperation = TypeOfOperation.HUE
        }
    }

    private fun applyManipulation(value: Float) {
        if(typeOfOperation == TypeOfOperation.Invalid) return
        if(imageStack.isEmpty()) return
        val bitmap: Bitmap?
        if(imageStack.getMaskMat() == null) {
            bitmap = when (typeOfOperation) {
                TypeOfOperation.BRIGHTNESS -> {
                    Operation.brightness(imageStack.getImageMat()!!, value.toDouble())
                }

                TypeOfOperation.CONTRAST -> {
                    Operation.contrast(imageStack.getImageMat()!!, value.toDouble())
                }

                TypeOfOperation.SATURATION -> {
                    Operation.saturation(imageStack.getImageMat()!!, value.toDouble())
                }

                TypeOfOperation.HUE -> {
                    Operation.hue(imageStack.getImageMat()!!, value.toDouble())
                }
                else -> null
            }
        }
        else{
            bitmap = when (typeOfOperation) {
                TypeOfOperation.BRIGHTNESS -> {
                    Operation.brightness(imageStack.getImageMat()!!, imageStack.getMaskMat()!!, value.toDouble())
                }

                TypeOfOperation.CONTRAST -> {
                    Operation.contrast(imageStack.getImageMat()!!, imageStack.getMaskMat()!!, value.toDouble())
                }

                TypeOfOperation.SATURATION -> {
                    Operation.saturation(imageStack.getImageMat()!!, imageStack.getMaskMat()!!, value.toDouble())
                }

                TypeOfOperation.HUE -> {
                    Operation.hue(imageStack.getImageMat()!!, imageStack.getMaskMat()!!, value.toDouble())
                }
                else -> null
            }
        }

        bitmap?.let {
            binding.imageView.setImageBitmap(it)
            edited = true
        }
    }

    private fun showSlider() {
        binding.slider.visibility = View.VISIBLE
        binding.slider.value = 0f
    }

    private fun hideSlider() {
        binding.slider.visibility = View.INVISIBLE
    }

    private fun sendCommandToServer(command: String) {
        apiClient.sendCommand(command, object : ApiCallback<EditResponse> {
            override fun onResult(t: EditResponse) {
                processEdit(t)
            }

            override fun onError(message: String) {
                Log.i("Error", message)
            }
        })
    }

    private fun processEdit(t: EditResponse) {
        typeOfOperation = TypeOfOperation.type[t.type] ?: TypeOfOperation.Invalid
        if (typeOfOperation == TypeOfOperation.Invalid) {
            Toast.makeText(this, "Invalid command", Toast.LENGTH_SHORT).show()
            return
        }
        applyManipulation(TypeOfOperation.scale[t.scale]!!.toFloat())
    }

    private fun setupToolBars() {
        binding.save.setOnClickListener{
            closeBottomSheet()
            if(!imageStack.isEmpty()){
                SaveImageToGallery.saveImage(this,
                    binding.imageView.drawable.toBitmapOrNull()!!,
                    "Articula"+System.currentTimeMillis().toString()
                )
            }
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
        }

        binding.undo.setOnClickListener {
            closeBottomSheet()
            imageStack.undo()
            if(imageStack.isEmpty()) {
                binding.imageView.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this,
                        R.drawable.round_image_24
                    )
                )
                return@setOnClickListener
            }
            binding.imageView.setImageBitmap(imageStack.peek())
        }

        binding.redo.setOnClickListener {
            closeBottomSheet()
            imageStack.redo()
            if(imageStack.isEmpty()) return@setOnClickListener
            binding.imageView.setImageBitmap(imageStack.peek())
        }
    }
}
