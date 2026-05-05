package com.livesense.japanese.tts

interface TextToSpeechListener {
    fun onDone()
    fun onError(message: String)
}

interface TextToSpeechManager {
    fun speak(text: String, listener: TextToSpeechListener)
    fun stop()
    fun shutdown()
}
