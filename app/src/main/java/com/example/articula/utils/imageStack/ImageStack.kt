package com.example.articula.utils.imageStack

import android.graphics.Bitmap
import com.example.articula.utils.editImage.ConvertType
import org.opencv.core.Mat
import java.util.Stack

class ImageStack {
    private val stack = Stack<Bitmap>()
    private val redoStack = Stack<Bitmap>()
    private var maskBitmap : Bitmap? = null
    private var imageMat : Mat? = null
    private var maskMat : Mat? = null

    fun push(bitmap: Bitmap) {
        stack.push(bitmap)
        redoStack.clear()
        imageMat = ConvertType.bitmapToMat(bitmap)
    }

    fun pop() {
        if(stack.isEmpty()){
            return
        }
        clearMask()
        stack.pop()
        if(stack.isEmpty()){
            imageMat = null
            return
        }
        imageMat = ConvertType.bitmapToMat(stack.peek())
    }

    fun undo(){
        if(stack.isEmpty()){
            return
        }
        redoStack.push(stack.peek())
        pop()
    }
    fun redo(){
        if(redoStack.isEmpty()){
            return
        }
        stack.push(redoStack.pop())
        imageMat = ConvertType.bitmapToMat(stack.peek())
    }

    fun peek(): Bitmap {
        return stack.peek()
    }

    fun isEmpty(): Boolean {
        return stack.isEmpty()
    }


    fun clear() {
        stack.clear()
        redoStack.clear()
        imageMat = null
        clearMask()
    }

    fun setMaskBitmap(bitmap: Bitmap){
        maskBitmap = bitmap
        maskMat = ConvertType.bitmapMaskToMatMask(bitmap)
    }

    fun getMaskBitmap(): Bitmap? {
        return maskBitmap
    }

    fun getImageMat(): Mat? {
        return imageMat
    }

    fun getMaskMat(): Mat? {
        return maskMat
    }

    fun clearMask(){
        maskBitmap = null
        maskMat = null
    }
}