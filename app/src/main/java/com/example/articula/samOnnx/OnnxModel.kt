package com.example.articula.samOnnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.articula.model.Point
import java.nio.FloatBuffer
import kotlin.math.max

class OnnxModel(context: Context) {
    private val environment = OrtEnvironment.getEnvironment()
    private val session : OrtSession
    private var embeddingTensor : OnnxTensor? = null
    init {
        val sessionOptions = OrtSession.SessionOptions()
        val onnxModel = context.assets.open("sam_onnx_quantized.onnx").use { it.readBytes() }
        session = environment.createSession(onnxModel,sessionOptions)
    }

    fun setEmbeddingTensor(data: FloatArray,shape: LongArray){
        embeddingTensor?.close()
        embeddingTensor = OnnxTensor.createTensor(environment,FloatBuffer.wrap(data),shape)
    }

    fun getMask(x : Float, y : Float, height : Int , width : Int): Bitmap? {
        if (embeddingTensor == null) return null

        val ortInputs = modelData(
            clicks = listOf(Point(x,y,1)),
            modelScale = ModelScale(height, width, (1024.0 / max(height,width)).toFloat())
        )

        val ortOutputs = session.run(ortInputs)
        val output = ortOutputs[session.outputNames.first()]

        val result = output.get() as OnnxTensor
        val maskData: FloatArray = result.floatBuffer.array()

        ortOutputs.close()
        ortInputs.forEach{
            if (it.key != "image_embeddings")
                it.value.close()
        }

        return floatArrayToBinaryMask(maskData,width,height)
    }

    private fun modelData(
        clicks: List<Point>,
        modelScale: ModelScale
    ): Map<String, OnnxTensor> {

        val n = clicks.size
        val pointCoords = FloatArray(2 * (n + 1))
        val pointLabels = FloatArray(n + 1)

        for (i in 0 until n) {
            val click = clicks[i]
            pointCoords[2 * i] = click.x * modelScale.samScale
            pointCoords[2 * i + 1] = click.y * modelScale.samScale
            pointLabels[i] = click.clickType.toFloat()
        }

        pointCoords[2 * n] = 0.0f
        pointCoords[2 * n + 1] = 0.0f
        pointLabels[n] = -1.0f

        val pointCoordsTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(pointCoords),
            longArrayOf(1, (n + 1).toLong(), 2)
        )

        val pointLabelsTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(pointLabels),
            longArrayOf(1, (n + 1).toLong())
        )

        val origImSizeTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(floatArrayOf(modelScale.height.toFloat(), modelScale.width.toFloat())),
            longArrayOf(2)
        )

        val maskInputTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.allocate(256 * 256),
            longArrayOf(1, 1, 256, 256)
        )

        val hasMaskInputTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(floatArrayOf(0.0f)),
            longArrayOf(1)
        )

        return mapOf(
            "image_embeddings" to embeddingTensor!!,
            "point_coords" to pointCoordsTensor,
            "point_labels" to pointLabelsTensor,
            "orig_im_size" to origImSizeTensor,
            "mask_input" to maskInputTensor,
            "has_mask_input" to hasMaskInputTensor
        )
    }

    private fun floatArrayToBinaryMask(input: FloatArray, width: Int, height: Int): Bitmap {
        val pixelsBinary = IntArray(width * height)

        for (i in input.indices) {
            val value = input[i]
            pixelsBinary[i] = if (value > 0.0f) Color.argb(255, 255, 255, 255) else Color.argb(255, 0, 0, 0)
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
}