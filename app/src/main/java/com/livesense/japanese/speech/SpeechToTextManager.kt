package com.livesense.japanese.speech

interface SpeechRecognitionListener {
    fun onPartialResult(text: String)
    fun onFinalResult(text: String)
    fun onError(message: String)
}

interface SpeechToTextManager {
    fun startListening(listener: SpeechRecognitionListener)
    fun stopListening()
    fun shutdown()
}
