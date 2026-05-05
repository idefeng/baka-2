package com.livesense.japanese.ui.chat

import com.livesense.japanese.llm.LlmManager
import com.livesense.japanese.speech.SpeechRecognitionListener
import com.livesense.japanese.speech.SpeechToTextManager
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
        assertEquals(ModelStatus.READY, viewModel.uiState.value.modelStatus)
    }

    @Test
    fun sendMessage_ignoresBlankInput() = runTest(dispatcher) {
        val viewModel = ChatViewModel(llmManager = FakeLlmManager("不会调用"))

        viewModel.sendMessage("   ")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals(ModelStatus.IDLE, viewModel.uiState.value.modelStatus)
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
        assertEquals(ModelStatus.ERROR, state.modelStatus)
        assertTrue(state.statusMessage.contains("model missing"))
    }

    @Test
    fun sendMessage_marksModelLoadingBeforeGenerationFinishes() = runTest(dispatcher) {
        val viewModel = ChatViewModel(llmManager = FakeLlmManager("回复"))

        viewModel.sendMessage("こんにちは")

        assertTrue(viewModel.uiState.value.isGenerating)
        assertEquals(ModelStatus.LOADING, viewModel.uiState.value.modelStatus)
        assertEquals("正在加载本地模型并生成回复...", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun startSpeechInput_whenManagerIsAvailable_marksListening() = runTest(dispatcher) {
        val speechManager = FakeSpeechToTextManager()
        val viewModel = ChatViewModel(
            llmManager = FakeLlmManager("回复"),
            speechToTextManager = speechManager,
        )

        viewModel.startSpeechInput()

        val state = viewModel.uiState.value
        assertEquals(SpeechStatus.LISTENING, state.speechStatus)
        assertEquals("正在听日语输入...", state.speechStatusMessage)
        assertTrue(speechManager.isListening)
    }

    @Test
    fun speechFinalResult_updatesInputTextAndStopsListening() = runTest(dispatcher) {
        val speechManager = FakeSpeechToTextManager()
        val viewModel = ChatViewModel(
            llmManager = FakeLlmManager("回复"),
            speechToTextManager = speechManager,
        )

        viewModel.startSpeechInput()
        speechManager.listener?.onFinalResult("今日はいい天気です")

        val state = viewModel.uiState.value
        assertEquals("今日はいい天気です", state.inputText)
        assertEquals(SpeechStatus.IDLE, state.speechStatus)
        assertFalse(speechManager.isListening)
    }

    @Test
    fun speechPartialResult_updatesInputTextAndKeepsListening() = runTest(dispatcher) {
        val speechManager = FakeSpeechToTextManager()
        val viewModel = ChatViewModel(
            llmManager = FakeLlmManager("回复"),
            speechToTextManager = speechManager,
        )

        viewModel.startSpeechInput()
        speechManager.listener?.onPartialResult("日本語を勉強しています")

        val state = viewModel.uiState.value
        assertEquals("日本語を勉強しています", state.inputText)
        assertEquals(SpeechStatus.LISTENING, state.speechStatus)
        assertTrue(speechManager.isListening)
    }

    @Test
    fun speechError_marksErrorAndStopsListening() = runTest(dispatcher) {
        val speechManager = FakeSpeechToTextManager()
        val viewModel = ChatViewModel(
            llmManager = FakeLlmManager("回复"),
            speechToTextManager = speechManager,
        )

        viewModel.startSpeechInput()
        speechManager.listener?.onError("模型文件不存在")

        val state = viewModel.uiState.value
        assertEquals(SpeechStatus.ERROR, state.speechStatus)
        assertEquals("语音识别失败：模型文件不存在", state.speechStatusMessage)
        assertFalse(speechManager.isListening)
    }

    private class FakeLlmManager(private val response: String) : LlmManager {
        override suspend fun generate(userInput: String): String = response
    }

    private class FailingLlmManager : LlmManager {
        override suspend fun generate(userInput: String): String {
            error("model missing")
        }
    }

    private class FakeSpeechToTextManager : SpeechToTextManager {
        var listener: SpeechRecognitionListener? = null
            private set
        var isListening = false
            private set

        override fun startListening(listener: SpeechRecognitionListener) {
            this.listener = listener
            isListening = true
        }

        override fun stopListening() {
            isListening = false
        }

        override fun shutdown() {
            isListening = false
            listener = null
        }
    }
}
