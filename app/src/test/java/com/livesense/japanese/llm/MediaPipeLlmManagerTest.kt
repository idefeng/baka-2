package com.livesense.japanese.llm

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPipeLlmManagerTest {
    @Test
    fun generate_wrapsPromptAndCallsEngine() = runTest {
        val engine = RecordingLlmEngine("【自然回应】こんにちは。\n【中文释义】你好。\n【用法场景】见面打招呼时使用。\n【纠错】没有明显错误。\n【更自然表达】こんにちは。")
        val manager = MediaPipeLlmManager(
            modelPath = "/data/local/tmp/llm/gemma.task",
            engineFactory = { engine },
        )

        val response = manager.generate("私は学生です")

        assertEquals("【自然回应】こんにちは。\n\n【中文释义】你好。\n\n【用法场景】见面打招呼时使用。\n\n【纠错】没有明显错误。\n\n【更自然表达】こんにちは。", response)
        assertTrue(engine.lastPrompt.contains("【自然回应】"))
        assertTrue(engine.lastPrompt.contains("用户输入：私は学生です"))
    }

    private class RecordingLlmEngine(private val response: String) : LlmEngine {
        var lastPrompt: String = ""

        override fun generate(prompt: String): String {
            lastPrompt = prompt
            return response
        }
    }
}
