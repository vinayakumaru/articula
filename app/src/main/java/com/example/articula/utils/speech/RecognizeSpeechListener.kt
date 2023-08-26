package com.example.articula.utils.speech

interface RecognizeSpeechListener {
    fun onListeningStarted()
    fun onListeningStopped()
    fun onResult(result: String)
}