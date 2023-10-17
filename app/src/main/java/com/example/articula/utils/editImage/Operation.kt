package com.example.articula.utils.editImage

import android.graphics.Bitmap
import android.graphics.Color
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.cvtColor


class Operation {
    companion object{
        fun brightness(imageMat: Mat, brightness: Double): Bitmap {
            val temp = imageMat.clone()
            temp.convertTo(temp, -1, 1.0, brightness*10)
            return ConvertType.matToBitmap(temp)
        }

        fun brightness(imageMat: Mat,maskMat: Mat, brightness: Double): Bitmap {
            val roiMat = Mat()
            val temp = imageMat.clone()
            temp.copyTo(roiMat, maskMat)
            roiMat.convertTo(roiMat, -1, 1.0, brightness*10)
            roiMat.copyTo(temp, maskMat)
            return ConvertType.matToBitmap(temp)
        }

        fun contrast(imageMat: Mat, contrast: Double): Bitmap {
            val temp = imageMat.clone()
            val value = (contrast+10)/10
            temp.convertTo(temp, -1, value, 0.0)
            return ConvertType.matToBitmap(temp)
        }

        fun contrast(imageMat: Mat,maskMat: Mat, contrast: Double): Bitmap {
            val temp = imageMat.clone()
            val roiMat = Mat()
            val value = (contrast+10)/10
            temp.copyTo(roiMat, maskMat)
            roiMat.convertTo(roiMat, -1, value, 0.0)
            roiMat.copyTo(temp, maskMat)
            return ConvertType.matToBitmap(temp)
        }

        fun saturation(imageMat: Mat, saturation: Double): Bitmap {
            val value = ((saturation + 10) / 20) * 2
            val temp = imageMat.clone()
            val hsv = Mat()
            cvtColor(temp, hsv, Imgproc.COLOR_RGB2HSV)
            val channels = ArrayList<Mat>()
            Core.split(hsv, channels)
            channels[1].convertTo(channels[1], -1, value, 0.0)
            Core.merge(channels, hsv)
            cvtColor(hsv, temp, Imgproc.COLOR_HSV2RGB, 4)
            return ConvertType.matToBitmap(temp)
        }


        fun saturation(imageMat: Mat, maskMat: Mat, saturation: Double): Bitmap {
            val value = ((saturation + 10) / 20) * 2
            val temp = imageMat.clone()
            val roiMat = Mat()
            temp.copyTo(roiMat, maskMat)

            val hsv = Mat()
            cvtColor(roiMat, hsv, Imgproc.COLOR_RGB2HSV)
            val channels = ArrayList<Mat>()
            Core.split(hsv, channels)
            channels[1].convertTo(channels[1], -1, value, 0.0)
            Core.merge(channels, hsv)
            cvtColor(hsv, roiMat, Imgproc.COLOR_HSV2RGB, 4)


            roiMat.copyTo(temp, maskMat)
            return ConvertType.matToBitmap(temp)
        }

        fun hue(imageMat: Mat, hue: Double): Bitmap {
            val value = ((hue + 10) / 20.0) * 3
            val temp = imageMat.clone()
            val hsv = Mat()
            cvtColor(temp, hsv, Imgproc.COLOR_RGB2HSV)
            val channels = ArrayList<Mat>()
            Core.split(hsv, channels)
            channels[0].convertTo(channels[0], -1, value, 0.0)
            Core.merge(channels, hsv)
            cvtColor(hsv, temp, Imgproc.COLOR_HSV2RGB, 4)
            return ConvertType.matToBitmap(temp)
        }

        fun hue(imageMat: Mat, maskMat: Mat, hue: Double): Bitmap {
            val value = ((hue + 10) / 20.0) * 3
            val temp = imageMat.clone()
            val roiMat = Mat()
            temp.copyTo(roiMat, maskMat)

            val hsv = Mat()
            cvtColor(roiMat, hsv, Imgproc.COLOR_RGB2HSV)
            val channels = ArrayList<Mat>()
            Core.split(hsv, channels)
            channels[0].convertTo(channels[0], -1, value, 0.0)
            Core.merge(channels, hsv)
            cvtColor(hsv, roiMat, Imgproc.COLOR_HSV2RGB, 4)


            roiMat.copyTo(temp, maskMat)
            return ConvertType.matToBitmap(temp)
        }

        fun blur(bitmap: Bitmap, blur: Double): Bitmap {
            val mat = ConvertType.bitmapToMat(bitmap)
            Imgproc.blur(mat, mat, org.opencv.core.Size(blur, blur))
            return ConvertType.matToBitmap(mat)
        }

        fun sharpen(bitmap: Bitmap, sharpen: Double): Bitmap {
            val mat = ConvertType.bitmapToMat(bitmap)
            val kernel = Mat(3, 3, CvType.CV_16SC1)
            kernel.put(0, 0, 0.0)
            kernel.put(0, 1, -1.0)
            kernel.put(0, 2, 0.0)
            kernel.put(1, 0, -1.0)
            kernel.put(1, 1, 5.0)
            kernel.put(1, 2, -1.0)
            kernel.put(2, 0, 0.0)
            kernel.put(2, 1, -1.0)
            kernel.put(2, 2, 0.0)
            Imgproc.filter2D(mat, mat, -1, kernel)
            return ConvertType.matToBitmap(mat)
        }

        fun edgeDetect(bitmap: Bitmap, edgeDetect: Double, dilationSize: Int): Bitmap {
            val mat = ConvertType.bitmapToMat(bitmap)
            val grayMat = Mat()
            cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

            val edges = Mat()
            Imgproc.Canny(grayMat, edges, edgeDetect, edgeDetect * 3)

            val element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(dilationSize.toDouble(), dilationSize.toDouble()))

            val dilatedEdges = Mat()
            Imgproc.dilate(edges, dilatedEdges, element)

            val res = Mat()
            Core.copyTo(mat, res, dilatedEdges)

            return ConvertType.matToBitmap(res)
        }



        fun grayScale(bitmap: Bitmap): Bitmap {
            val mat = ConvertType.bitmapToMat(bitmap)
            val res = Mat(bitmap.height, bitmap.width, CvType.CV_8UC3)

            cvtColor(mat, res, Imgproc.COLOR_RGB2GRAY)
            return ConvertType.matToBitmap(res)
        }
    }
}