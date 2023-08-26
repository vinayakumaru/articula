package com.example.articula.utils.editImage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

class AddMaskToImage {
    companion object{
        fun addMaskToImage(bitmap: Bitmap, mask: Bitmap): Bitmap{
            val modifiedMask = modifyBitmap(mask)
            val modifiedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            val canvas = Canvas(modifiedBitmap)
            val paint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.drawBitmap(modifiedMask, 0f, 0f, paint)
            return modifiedBitmap
        }
        private fun modifyBitmap(originalBitmap: Bitmap): Bitmap {
            val width = originalBitmap.width
            val height = originalBitmap.height
            val modifiedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixelColor = originalBitmap.getPixel(x, y)
                    val newPixelColor = if (pixelColor == Color.argb(255, 255, 255, 255)) {
                        Color.argb(100, 0, 0, 0)
                    } else {
                        Color.argb(255, 0, 0, 0)
                    }
                    modifiedBitmap.setPixel(x, y, newPixelColor)
                }
            }
            return modifiedBitmap
        }
    }

}