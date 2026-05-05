package com.livesense.japanese.ui.chat

import com.livesense.japanese.data.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val modelStatus: ModelStatus = ModelStatus.IDLE,
    val statusMessage: String = "本地模型待命",
)

enum class ModelStatus {
    IDLE,
    LOADING,
    READY,
    ERROR,
}
