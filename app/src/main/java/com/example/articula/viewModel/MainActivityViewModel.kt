package com.example.articula.viewModel

import androidx.lifecycle.ViewModel
import com.example.articula.samOnnx.OnnxModel
import com.example.articula.utils.imageStack.ImageStack

class MainActivityViewModel : ViewModel() {
    val imageStack : ImageStack = ImageStack()
    var onnxModel : OnnxModel? = null
}