package com.livesense.japanese.llm

fun interface LlmEngineFactory {
    fun create(modelPath: String): LlmEngine
}

interface LlmEngine {
    fun generate(prompt: String): String
}
