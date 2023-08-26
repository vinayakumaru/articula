package com.example.articula.utils.imageIO

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri

/**
* This class is used to get a bitmap from an uri
* @param context: Context of the activity
 */
class ImageFromUri(private val context: Context) {
    /**
     * This function is used to get a image bitmap from an uri
     * @param uri: Uri of the image
     * @return Bitmap of the image or null if the image can't be found
     */
    fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            ImageDecoder.createSource(context.contentResolver, uri).let {
                ImageDecoder.decodeBitmap(it)
            }
        } catch (e: Exception) {
            null
        }
    }
}