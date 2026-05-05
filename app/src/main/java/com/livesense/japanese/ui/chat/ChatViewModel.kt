package com.livesense.japanese.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesense.japanese.data.ChatMessage
import com.livesense.japanese.data.MessageRole
import com.livesense.japanese.llm.LlmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val llmManager: LlmManager,
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

    fun sendMessage(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty() || uiState.value.isGenerating) return

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
            val generationResult = runCatching { llmManager.generate(input) }
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

    private fun buildLlmErrorMessage(error: Throwable): String {
        // 把模型加载/推理异常转成可读提示，避免 UI 一直停留在生成中。
        return """
            【自然回应】
            すみません、今は本地模型を読み込めません。

            【纠错】
            本地模型调用失败：${error.message ?: "未知错误"}。请确认设备上存在 MediaPipe .task 模型文件。

            【更自然表达】
            もう一度お願いします。
        """.trimIndent()
    }
}
