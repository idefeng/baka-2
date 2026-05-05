package com.livesense.japanese.llm

interface LlmManager {
    suspend fun generate(userInput: String): String
}
