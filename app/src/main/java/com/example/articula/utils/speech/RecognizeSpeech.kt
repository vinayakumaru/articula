package com.example.articula.utils.speech

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import com.example.articula.utils.permission.Permission
import java.util.Locale

/**
 * This class is used to recognize speech
 * @param context: Context of the activity
 * @property speechRecognizer: {@link SpeechRecognizer}
 * @property speechRecognizerIntent: {@link SpeechRecognizerIntent}
 * @property listener: A callback to know when the speech recognizer is listening, when it stops and when it has a result
 * @property isListening: Boolean used to know if the speech recognizer is listening
 *
 */
class RecognizeSpeech(private val context: Context){
    private var speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var speechRecognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    private var listener: RecognizeSpeechListener? = null
    private var isListening = false

    init {
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                listener?.onListeningStarted()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                listener?.onListeningStopped()
            }

            override fun onError(error: Int) {
                isListening = false
                listener?.onListeningStopped()
            }

            override fun onResults(results: Bundle?) {
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (data != null) {
                    listener?.onResult(data[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun setListener(listener: RecognizeSpeechListener){
        this.listener = listener
    }

    /**
     * This function is used to start or stop the speech recognizer
     */
    fun clicked(){
        if(Permission(context).isPermissionGranted(Permission.RECORD_AUDIO)){
            if (!isListening) {
                speechRecognizer.startListening(speechRecognizerIntent)
            } else {
                speechRecognizer.stopListening()
                isListening = false
                listener?.onListeningStopped()
            }
        }
        else{
            Permission(context).requestPermissions(listOf(Permission.RECORD_AUDIO), object : Permission.PermissionListener {
                override fun onPermissionsGranted() {
                    clicked()
                }
                override fun onPermissionsDenied(deniedPermissions: List<String>) {
                    Toast.makeText(
                        context,
                        "You have denied the permission to record audio",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}