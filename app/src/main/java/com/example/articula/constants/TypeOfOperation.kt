package com.example.articula.constants

class TypeOfOperation {
    companion object {
        const val Invalid = -1
        const val BRIGHTNESS = 0
        const val CONTRAST = 1
        const val SATURATION = 2
        const val UNDO = 3
        const val CROP = 4
        const val REDO = 5
        const val HUE = 6
        val type = mapOf(
            "invalid" to -1,
            "brightness" to 0,
            "contrast" to 1,
            "saturation" to 2,
            "undo" to 3,
            "crop" to 4,
            "redo" to 5,
            "hue" to 6,
        )
        val scale = mapOf(
            "increase" to 5,
            "decrease" to -5,
        )
    }
}