package com.example.articula.utils.editImage

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object ConvertType {
    fun matToBitmap(mat: Mat): Bitmap {
        val bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bmp)
        return bmp
    }

    fun bitmapToMat(bmp: Bitmap): Mat {
        val bitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    fun bitmapMaskToMatMask(bmp: Bitmap): Mat {
        val bitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

//    fun bitmapMaskToMatMask(bitmap: Bitmap): Mat {
//        val bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//
//        // Convert the Android Bitmap to an OpenCV Mat
//        val mat = Mat(bmp.height, bmp.width, CvType.CV_8UC3)
//        Utils.bitmapToMat(bmp, mat)
//
//        // Define the colors you want to map
//        val color1 = Scalar(0.0, 0.0, 0.0, 100.0)  // Color.argb(100, 0, 0, 0)
//        val color2 = Scalar(0.0, 0.0, 0.0, 255.0)  // Color.argb(255, 0, 0, 0)
//
//        // Create a binary mask based on the specified color conditions
//        val binaryMask = Mat()
//        Core.inRange(mat, color1, color2, binaryMask)
//
//        // Create a mask for the pixels matching color1
//        val maskColor1 = Mat()
//        Core.inRange(mat, color1, color1, maskColor1)
//
//        // Set the pixels in the binary mask corresponding to color1 to white (255)
//        Core.bitwise_and(binaryMask, maskColor1, binaryMask)
//
//        return binaryMask
//    }
}