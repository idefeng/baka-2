package com.livesense.japanese.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesense.japanese.data.ChatMessage
import com.livesense.japanese.data.MessageRole
import com.livesense.japanese.llm.LlmManager
import com.livesense.japanese.speech.SpeechRecognitionListener
import com.livesense.japanese.speech.SpeechToTextManager
import com.livesense.japanese.tts.TextToSpeechListener
import com.livesense.japanese.tts.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val llmManager: LlmManager,
    private val speechToTextManager: SpeechToTextManager? = null,
    private val textToSpeechManager: TextToSpeechManager? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.update { currentState ->
            currentState.copy(inputText = text)
        }
    }

    fun sendCurrentInput() {
        sendMessage(uiState.value.inputText)
    }

    fun clearMessages() {
        if (uiState.value.isGenerating || uiState.value.speechStatus == SpeechStatus.LISTENING) return

        _uiState.update { currentState ->
            // 只清空历史消息，保留输入框和模型状态，避免误删用户正在编辑的内容。
            currentState.copy(messages = emptyList())
        }
    }

    fun sendMessage(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty() || uiState.value.isGenerating) return

        val recentMessages = uiState.value.messages.takeLast(MAX_CONTEXT_MESSAGES)
        val userMessage = ChatMessage(role = MessageRole.USER, content = input)
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                inputText = "",
                isGenerating = true,
                modelStatus = ModelStatus.LOADING,
                statusMessage = "正在加载本地模型并生成回复...",
            )
        }

        viewModelScope.launch {
            // 串联用户输入与本地 LLM，生成完成后追加 AI 消息。
            val generationResult = runCatching {
                llmManager.generate(
                    userInput = input,
                    recentMessages = recentMessages,
                )
            }
            val aiContent = generationResult.getOrElse { error ->
                buildLlmErrorMessage(error)
            }
            val aiMessage = ChatMessage(role = MessageRole.AI, content = aiContent)
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + aiMessage,
                    isGenerating = false,
                    modelStatus = if (generationResult.isSuccess) ModelStatus.READY else ModelStatus.ERROR,
                    statusMessage = if (generationResult.isSuccess) {
                        "本地模型已就绪"
                    } else {
                        "本地模型调用失败：${generationResult.exceptionOrNull()?.message ?: "未知错误"}"
                    },
                )
            }
        }
    }

    fun startSpeechInput() {
        val manager = speechToTextManager
        if (manager == null || uiState.value.isGenerating || uiState.value.speechStatus == SpeechStatus.LISTENING) return

        _uiState.update { currentState ->
            currentState.copy(
                speechStatus = SpeechStatus.LISTENING,
                speechStatusMessage = "正在听日语输入...",
            )
        }

        // 将平台语音识别结果收敛到输入框，用户可编辑后再发送给 LLM。
        manager.startListening(
            object : SpeechRecognitionListener {
                override fun onPartialResult(text: String) {
                    if (text.isBlank()) return
                    _uiState.update { currentState ->
                        currentState.copy(inputText = text)
                    }
                }

                override fun onFinalResult(text: String) {
                    manager.stopListening()
                    _uiState.update { currentState ->
                        currentState.copy(
                            inputText = text.ifBlank { currentState.inputText },
                            speechStatus = SpeechStatus.IDLE,
                            speechStatusMessage = "语音输入待命",
                        )
                    }
                }

                override fun onError(message: String) {
                    manager.stopListening()
                    _uiState.update { currentState ->
                        currentState.copy(
                            speechStatus = SpeechStatus.ERROR,
                            speechStatusMessage = "语音识别失败：$message",
                        )
                    }
                }
            }
        )
    }

    fun stopSpeechInput() {
        speechToTextManager?.stopListening()
        _uiState.update { currentState ->
            currentState.copy(
                speechStatus = SpeechStatus.IDLE,
                speechStatusMessage = "语音输入待命",
            )
        }
    }

    fun onSpeechPermissionDenied() {
        _uiState.update { currentState ->
            currentState.copy(
                speechStatus = SpeechStatus.ERROR,
                speechStatusMessage = "语音识别失败：需要麦克风权限",
            )
        }
    }

    fun speakMessage(message: ChatMessage) {
        val manager = textToSpeechManager ?: return
        if (message.role != MessageRole.AI || uiState.value.isGenerating || uiState.value.speechStatus == SpeechStatus.LISTENING) return

        val speechText = extractNaturalResponse(message.content)
        if (speechText.isBlank()) return

        _uiState.update { currentState ->
            currentState.copy(
                ttsStatus = TtsStatus.SPEAKING,
                ttsStatusMessage = "正在播放日语回应...",
            )
        }

        manager.speak(
            speechText,
            object : TextToSpeechListener {
                override fun onDone() {
                    _uiState.update { currentState ->
                        currentState.copy(
                            ttsStatus = TtsStatus.IDLE,
                            ttsStatusMessage = "语音播放待命",
                        )
                    }
                }

                override fun onError(message: String) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            ttsStatus = TtsStatus.ERROR,
                            ttsStatusMessage = "语音播放失败：$message",
                        )
                    }
                }
            }
        )
    }

    fun stopSpeaking() {
        textToSpeechManager?.stop()
        _uiState.update { currentState ->
            currentState.copy(
                ttsStatus = TtsStatus.IDLE,
                ttsStatusMessage = "语音播放待命",
            )
        }
    }

    override fun onCleared() {
        speechToTextManager?.shutdown()
        textToSpeechManager?.shutdown()
        super.onCleared()
    }

    private fun buildLlmErrorMessage(error: Throwable): String {
        // 把模型加载/推理异常转成可读提示，避免 UI 一直停留在生成中。
        return """
            【用户原句意思】
            本地模型暂时无法分析这句话。

            【自然回应】
            すみません、今は本地モデルを読み込めません。

            【回应意思】
            抱歉，现在无法加载本地模型。

            【语法分析】
            本地模型调用失败：${error.message ?: "未知错误"}。请确认设备上存在 MediaPipe .task 模型文件。
        """.trimIndent()
    }

    private fun extractNaturalResponse(content: String): String {
        val startLabel = "【自然回应】"
        val endLabels = listOf("【回应意思】", "【语法分析】", "【纠错】", "【更自然表达】")
        val start = content.indexOf(startLabel)
        if (start < 0) return content.trim()

        val contentStart = start + startLabel.length
        val contentEnd = endLabels
            .mapNotNull { label -> content.indexOf(label, contentStart).takeIf { it >= 0 } }
            .minOrNull()
            ?: content.length
        return content.substring(contentStart, contentEnd).trim()
    }

    private companion object {
        private const val MAX_CONTEXT_MESSAGES = 6
    }
}
