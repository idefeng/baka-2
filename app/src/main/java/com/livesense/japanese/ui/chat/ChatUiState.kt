package com.livesense.japanese.ui.chat

import com.livesense.japanese.data.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
)
