package com.livesense.japanese.ui.chat

import com.livesense.japanese.data.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val modelStatus: ModelStatus = ModelStatus.IDLE,
    val statusMessage: String = "本地模型待命",
    val speechStatus: SpeechStatus = SpeechStatus.IDLE,
    val speechStatusMessage: String = "语音输入待命",
    val ttsStatus: TtsStatus = TtsStatus.IDLE,
    val ttsStatusMessage: String = "语音播放待命",
)

enum class ModelStatus {
    IDLE,
    LOADING,
    READY,
    ERROR,
}

enum class SpeechStatus {
    IDLE,
    LISTENING,
    ERROR,
}

enum class TtsStatus {
    IDLE,
    SPEAKING,
    ERROR,
}
