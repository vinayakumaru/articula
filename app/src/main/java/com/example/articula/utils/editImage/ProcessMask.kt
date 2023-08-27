package com.example.articula.utils.editImage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

class ProcessMask {
    companion object{
        fun getBinaryMask(intArray: IntArray, width: Int, height: Int): Bitmap {
            val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            mask.setPixels(intArray, 0, width, 0, 0, width, height)
            return mask
        }
        fun addMaskToImage(bitmap: Bitmap, mask : Bitmap): Bitmap{
            val modifiedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            val canvas = Canvas(modifiedBitmap)
            val paint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.drawBitmap(mask, 0f, 0f, paint)
            return modifiedBitmap
        }
    }
}