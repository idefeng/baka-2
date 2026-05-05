package com.livesense.japanese.ui.chat

import com.livesense.japanese.llm.LlmManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendMessage_appendsUserMessageThenAiResponse() = runTest(dispatcher) {
        val viewModel = ChatViewModel(
            llmManager = FakeLlmManager("【自然回应】こんにちは\n【纠错】没有明显错误\n【更自然表达】こんにちは。")
        )

        viewModel.sendMessage("こんにちは")
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("こんにちは", messages[0].content)
        assertEquals("【自然回应】こんにちは\n【纠错】没有明显错误\n【更自然表达】こんにちは。", messages[1].content)
        assertFalse(viewModel.uiState.value.isGenerating)
    }

    @Test
    fun sendMessage_ignoresBlankInput() = runTest(dispatcher) {
        val viewModel = ChatViewModel(llmManager = FakeLlmManager("不会调用"))

        viewModel.sendMessage("   ")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun sendMessage_whenLlmFails_appendsErrorMessageAndStopsGenerating() = runTest(dispatcher) {
        val viewModel = ChatViewModel(llmManager = FailingLlmManager())

        viewModel.sendMessage("こんにちは")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertTrue(state.messages[1].content.contains("本地模型调用失败"))
        assertFalse(state.isGenerating)
    }

    private class FakeLlmManager(private val response: String) : LlmManager {
        override suspend fun generate(userInput: String): String = response
    }

    private class FailingLlmManager : LlmManager {
        override suspend fun generate(userInput: String): String {
            error("model missing")
        }
    }
}
