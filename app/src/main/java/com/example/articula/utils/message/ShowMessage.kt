package com.example.articula.utils.message

import android.content.Context
import android.widget.Toast

class ShowMessage {
    companion object {
        fun show(context: Context,message: String) {
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}