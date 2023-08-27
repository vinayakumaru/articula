package com.example.articula.utils.imageIO

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

class SaveImageInCache {
    companion object{
        fun getImage(context: Context,bitmap: Bitmap,fileName : String): File {
            val file = File(context.applicationContext.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            return file
        }
    }
}