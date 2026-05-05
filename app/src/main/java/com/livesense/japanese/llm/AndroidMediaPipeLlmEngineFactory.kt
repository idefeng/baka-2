package com.livesense.japanese.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession

class AndroidMediaPipeLlmEngineFactory(
    private val context: Context,
) : LlmEngineFactory {
    override fun create(modelPath: String): LlmEngine {
        // MediaPipe LLM 需要 .task 模型包，开发阶段建议用 adb 放到 /data/local/tmp/llm/。
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            // maxTokens 包含输入和输出；五段学习 Prompt 本身会超过 256 tokens。
            .setMaxTokens(1024)
            .setMaxTopK(32)
            .build()
        val inference = LlmInference.createFromOptions(context, options)

        return MediaPipeLlmEngine(inference)
    }
}

private class MediaPipeLlmEngine(
    private val inference: LlmInference,
) : LlmEngine {
    override fun generate(prompt: String): String {
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(8)
            .setTopP(0.75f)
            .setTemperature(0.2f)
            .setRandomSeed(7)
            .build()

        // generateResponse 是阻塞调用，外层 Manager 已切到后台线程执行。
        LlmInferenceSession.createFromOptions(inference, sessionOptions).use { session ->
            session.addQueryChunk(prompt)
            return session.generateResponse()
        }
    }
}
