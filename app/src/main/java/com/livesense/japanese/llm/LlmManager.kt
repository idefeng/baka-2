package com.livesense.japanese.llm

import com.livesense.japanese.data.ChatMessage

interface LlmManager {
    suspend fun generate(
        userInput: String,
        recentMessages: List<ChatMessage> = emptyList(),
    ): String
}
