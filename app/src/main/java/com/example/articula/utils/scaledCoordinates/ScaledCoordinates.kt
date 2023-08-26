package com.example.articula.utils.scaledCoordinates

import android.content.res.Configuration

class ScaledCoordinates(private val orientation : Int) {
    private var isValid : Boolean? = null
    private var x : Float? = null
    private var y : Float? = null

    fun getIsValid() : Boolean{
        return isValid!!
    }

    fun getX() : Float{
        return x!!
    }

    fun getY() : Float{
        return y!!
    }
    fun check(eventX:Float,eventY:Float,imageHeight:Int,imageWidth:Int,viewHeight:Int,viewWidth:Int){
        val scaleW : Float = viewWidth / imageWidth.toFloat()
        val scaleH : Float = viewHeight / imageHeight.toFloat()

        val scale = getScale(scaleW,scaleH)

        val scaledImageWidth = imageWidth * scale
        val scaledImageHeight = imageHeight * scale

        val x = eventX - (viewWidth - scaledImageWidth) / 2
        val y = eventY - (viewHeight - scaledImageHeight) / 2

        if (x < 0 || y < 0 || x > scaledImageWidth || y > scaledImageHeight) {
            isValid = false
            this.x = 0f
            this.y = 0f
        }
        else{
            this.isValid = true
            this.x = x/scale
            this.y = y/scale
        }
    }

    private fun getScale(scaleW: Float, scaleH: Float) : Float{
        return when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                scaleW
            }
            else -> {
                scaleH
            }
        }
    }
}