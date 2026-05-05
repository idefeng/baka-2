package com.livesense.japanese.speech

import android.content.Context
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class VoskSpeechToTextManager(
    private val context: Context,
) : SpeechToTextManager {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var pendingStartListener: SpeechRecognitionListener? = null
    private var isModelLoading = false

    override fun startListening(listener: SpeechRecognitionListener) {
        pendingStartListener = listener
        val loadedModel = model
        if (loadedModel != null) {
            startSpeechService(loadedModel, listener)
            return
        }

        if (isModelLoading) return
        isModelLoading = true

        val validationError = VoskModelAssetValidator.validate(listModelAssetFiles().toSet())
        if (validationError != null) {
            isModelLoading = false
            pendingStartListener = null
            listener.onError(validationError)
            return
        }

        // Vosk Android 要求把 assets/model 解包到应用私有目录后再加载。
        StorageService.unpack(
            context,
            MODEL_ASSET_DIR,
            MODEL_TARGET_DIR,
            { unpackedModel ->
                isModelLoading = false
                model = unpackedModel
                pendingStartListener?.let { startSpeechService(unpackedModel, it) }
            },
            { error ->
                isModelLoading = false
                pendingStartListener = null
                listener.onError(error.message ?: "无法加载 Vosk 模型，请确认 app/src/main/assets/model 已放入模型文件")
            },
        )
    }

    override fun stopListening() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        pendingStartListener = null
    }

    override fun shutdown() {
        stopListening()
        model?.close()
        model = null
    }

    private fun startSpeechService(
        loadedModel: Model,
        listener: SpeechRecognitionListener,
    ) {
        try {
            speechService?.shutdown()
            speechService = SpeechService(Recognizer(loadedModel, SAMPLE_RATE), SAMPLE_RATE).also { service ->
                service.startListening(VoskListener(listener))
            }
        } catch (error: IOException) {
            listener.onError(error.message ?: "无法启动麦克风识别")
        }
    }

    private fun listModelAssetFiles(
        assetPath: String = MODEL_ASSET_DIR,
        relativePath: String = "",
    ): List<String> {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            return if (relativePath.isBlank()) emptyList() else listOf(relativePath)
        }

        return children.flatMap { child ->
            val nextAssetPath = "$assetPath/$child"
            val nextRelativePath = if (relativePath.isBlank()) child else "$relativePath/$child"
            listModelAssetFiles(nextAssetPath, nextRelativePath)
        }
    }

    private class VoskListener(
        private val listener: SpeechRecognitionListener,
    ) : RecognitionListener {
        private val stableSegments = mutableListOf<String>()

        override fun onPartialResult(hypothesis: String) {
            parseText(hypothesis, PARTIAL_TEXT_KEY)?.let { partialText ->
                listener.onPartialResult(joinSegments(stableSegments + partialText))
            }
        }

        override fun onResult(hypothesis: String) {
            // onResult 是一次停顿后的稳定片段，不代表整次录音结束；继续监听能保留更多上下文。
            parseText(hypothesis, FINAL_TEXT_KEY)?.let { resultText ->
                stableSegments += resultText
                listener.onPartialResult(joinSegments(stableSegments))
            }
        }

        override fun onFinalResult(hypothesis: String) {
            parseText(hypothesis, FINAL_TEXT_KEY)?.let { finalText ->
                stableSegments += finalText
            }
            listener.onFinalResult(joinSegments(stableSegments))
        }

        override fun onError(exception: Exception) {
            listener.onError(exception.message ?: "语音识别异常")
        }

        override fun onTimeout() {
            listener.onFinalResult("")
        }

        private fun parseText(json: String, key: String): String? {
            // Vosk 返回 JSON 字符串，只提取 text/partial 字段给业务层。
            return runCatching { JSONObject(json).optString(key).trim() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }

        private fun joinSegments(segments: List<String>): String {
            return segments.filter { it.isNotBlank() }.joinToString("。")
        }
    }

    private companion object {
        private const val MODEL_ASSET_DIR = "model"
        private const val MODEL_TARGET_DIR = "vosk-model"
        private const val SAMPLE_RATE = 16_000.0f
        private const val PARTIAL_TEXT_KEY = "partial"
        private const val FINAL_TEXT_KEY = "text"
    }
}
