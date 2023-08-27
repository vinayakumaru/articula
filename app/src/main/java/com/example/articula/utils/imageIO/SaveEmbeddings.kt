package com.example.articula.utils.imageIO

import android.content.Context
import java.io.File

class SaveEmbeddings {
    companion object{
        fun saveEmbeddings(context : Context, embeddings: ByteArray, fileName: String){
            val file = File(context.filesDir, fileName)
            file.writeBytes(embeddings)
        }

        fun getEmbeddings(context: Context, fileName: String): ByteArray? {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                return file.readBytes()
            }
            return null
        }

    }
}