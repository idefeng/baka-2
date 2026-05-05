package com.livesense.japanese.llm

import com.livesense.japanese.prompt.JapaneseTeacherPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MediaPipeLlmManager(
    private val modelPath: String = DEFAULT_MODEL_PATH,
    private val engineFactory: LlmEngineFactory,
) : LlmManager {
    private val engineMutex = Mutex()
    private var engine: LlmEngine? = null

    override suspend fun generate(userInput: String): String = withContext(Dispatchers.Default) {
        val prompt = JapaneseTeacherPrompt.build(userInput)

        // 首次调用时加载模型，后续复用同一个推理引擎，避免每条消息重复加载大模型。
        val currentEngine = engineMutex.withLock {
            engine ?: engineFactory.create(modelPath).also { engine = it }
        }

        LlmResponseFormatter.format(currentEngine.generate(prompt))
    }

    companion object {
        const val DEFAULT_MODEL_PATH = "/data/local/tmp/llm/gemma.task"
    }
}
