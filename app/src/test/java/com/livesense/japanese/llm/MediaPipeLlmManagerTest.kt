package com.livesense.japanese.llm

import com.livesense.japanese.data.ChatMessage
import com.livesense.japanese.data.MessageRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPipeLlmManagerTest {
    @Test
    fun generate_wrapsPromptWithRecentMessagesAndCallsEngine() = runTest {
        val engine = RecordingLlmEngine(
            "【用户原句意思】我是学生。\n【自然回应】そうですか。\n【回应意思】这样啊。\n【语法分析】「私は」是主语，「学生です」是判断句。"
        )
        val manager = MediaPipeLlmManager(
            modelPath = "/data/local/tmp/llm/gemma.task",
            engineFactory = { engine },
        )

        val response = manager.generate(
            userInput = "私は学生です",
            recentMessages = listOf(ChatMessage(role = MessageRole.USER, content = "こんにちは")),
        )

        assertEquals("【用户原句意思】我是学生。\n\n【自然回应】そうですか。\n\n【回应意思】这样啊。\n\n【语法分析】「私は」是主语，「学生です」是判断句。", response)
        assertTrue(engine.lastPrompt.contains("【用户原句意思】"))
        assertTrue(engine.lastPrompt.contains("【自然回应】"))
        assertTrue(engine.lastPrompt.contains("【回应意思】"))
        assertTrue(engine.lastPrompt.contains("【语法分析】"))
        assertTrue(engine.lastPrompt.contains("最近对话："))
        assertTrue(engine.lastPrompt.contains("用户：こんにちは"))
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
