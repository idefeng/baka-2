package com.livesense.japanese.ui.chat

import com.livesense.japanese.data.ChatMessage
import com.livesense.japanese.data.MessageRole
import com.livesense.japanese.llm.LlmManager
import com.livesense.japanese.speech.SpeechRecognitionListener
import com.livesense.japanese.speech.SpeechToTextManager
import com.livesense.japanese.tts.TextToSpeechListener
import com.livesense.japanese.tts.TextToSpeechManager
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
            llmManager = FakeLlmManager("【用户原句意思】你好\n【自然回应】こんにちは\n【回应意思】你好\n【语法分析】问候语。")
        )

        viewModel.sendMessage("こんにちは")
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("こんにちは", messages[0].content)
        assertEquals("【用户原句意思】你好\n【自然回应】こんにちは\n【回应意思】你好\n【语法分析】问候语。", messages[1].content)
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
    fun clearMessages_removesConversationAndKeepsCurrentInput() = runTest(dispatcher) {
        val viewModel = ChatViewModel(llmManager = FakeLlmManager("回复"))

        viewModel.sendMessage("こんにちは")
        advanceUntilIdle()
        viewModel.onInputChange("次の質問")
        viewModel.clearMessages()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("次の質問", state.inputText)
        assertEquals(ModelStatus.READY, state.modelStatus)
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

    @Test
    fun speakMessage_whenAiMessageHasNaturalResponse_speaksOnlyJapaneseResponse() = runTest(dispatcher) {
        val ttsManager = FakeTextToSpeechManager()
        val viewModel = ChatViewModel(
            llmManager = FakeLlmManager("回复"),
            textToSpeechManager = ttsManager,
        )
        val aiMessage = ChatMessage(
            role = MessageRole.AI,
            content = "【用户原句意思】我什么也不想吃。\n\n【自然回应】そうなんですね。無理しなくていいですよ。\n\n【回应意思】这样啊。不勉强自己也可以。\n\n【语法分析】「何も」和否定表达连用。",
        )

        viewModel.speakMessage(aiMessage)

        assertEquals("そうなんですね。無理しなくていいですよ。", ttsManager.spokenText)
        assertEquals(TtsStatus.SPEAKING, viewModel.uiState.value.ttsStatus)
        assertEquals("正在播放日语回应...", viewModel.uiState.value.ttsStatusMessage)
    }

    @Test
    fun ttsError_marksErrorStatus() = runTest(dispatcher) {
        val ttsManager = FakeTextToSpeechManager()
        val viewModel = ChatViewModel(
            llmManager = FakeLlmManager("回复"),
            textToSpeechManager = ttsManager,
        )
        val aiMessage = ChatMessage(role = MessageRole.AI, content = "【自然回应】こんにちは。")

        viewModel.speakMessage(aiMessage)
        ttsManager.listener?.onError("TTS 未初始化")

        assertEquals(TtsStatus.ERROR, viewModel.uiState.value.ttsStatus)
        assertEquals("语音播放失败：TTS 未初始化", viewModel.uiState.value.ttsStatusMessage)
    }

    private class FakeLlmManager(private val response: String) : LlmManager {
        override suspend fun generate(
            userInput: String,
            recentMessages: List<ChatMessage>,
        ): String = response
    }

    private class FailingLlmManager : LlmManager {
        override suspend fun generate(
            userInput: String,
            recentMessages: List<ChatMessage>,
        ): String {
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

    private class FakeTextToSpeechManager : TextToSpeechManager {
        var spokenText: String = ""
            private set
        var listener: TextToSpeechListener? = null
            private set

        override fun speak(text: String, listener: TextToSpeechListener) {
            spokenText = text
            this.listener = listener
        }

        override fun stop() = Unit

        override fun shutdown() {
            listener = null
        }
    }
}
