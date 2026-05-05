package com.livesense.japanese.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AndroidTextToSpeechManager(
    context: Context,
) : TextToSpeechManager {
    private var listener: TextToSpeechListener? = null
    private var isReady = false
    private var initError: String? = null
    private val tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            configureJapaneseVoice()
        } else {
            initError = "系统 TTS 初始化失败"
        }
    }

    init {
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    listener?.onDone()
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String?) {
                    listener?.onError("系统 TTS 播放失败")
                }
            }
        )
    }

    override fun speak(text: String, listener: TextToSpeechListener) {
        this.listener = listener
        val error = initError
        if (error != null) {
            listener.onError(error)
            return
        }
        if (!isReady) {
            listener.onError("系统 TTS 尚未就绪")
            return
        }

        // 只朗读日语回应，不朗读中文解释，避免学习信息被混成一段音频。
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun stop() {
        tts.stop()
        listener = null
    }

    override fun shutdown() {
        stop()
        tts.shutdown()
    }

    private fun configureJapaneseVoice() {
        val availability = tts.setLanguage(Locale.JAPANESE)
        if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            initError = "系统 TTS 不支持日语，请安装日语语音数据"
            return
        }
        tts.setSpeechRate(0.9f)
        isReady = true
    }

    private companion object {
        private const val UTTERANCE_ID = "livesense_japanese_ai_response"
    }
}
