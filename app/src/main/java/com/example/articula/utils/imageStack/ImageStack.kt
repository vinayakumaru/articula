package com.example.articula.utils.imageStack

import android.graphics.Bitmap
import java.util.Stack

class ImageStack {
    private val stack = Stack<Bitmap>()
    private var maskBitmap : Bitmap? = null

    fun push(bitmap: Bitmap) {
        stack.push(bitmap)
    }

    fun pop(): Bitmap {
        return stack.pop()
    }

    fun peek(): Bitmap {
        return stack.peek()
    }

    fun isEmpty(): Boolean {
        return stack.isEmpty()
    }

    fun size(): Int {
        return stack.size
    }

    fun clear() {
        stack.clear()
    }

    fun setMaskBitmap(bitmap: Bitmap){
        maskBitmap = bitmap
    }

    fun getMaskBitmap(): Bitmap? {
        return maskBitmap
    }

    fun clearMask(){
        maskBitmap = null
    }
}