package com.livesense.japanese.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.livesense.japanese.data.ChatMessage
import com.livesense.japanese.data.MessageRole

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val speechPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.startSpeechInput()
        } else {
            viewModel.onSpeechPermissionDenied()
        }
    }

    fun requestSpeechInput() {
        // 录音权限只在用户点击语音按钮时申请，避免启动时打断文本输入。
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startSpeechInput()
        } else {
            speechPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ModelStatusBar(
            modelStatus = uiState.modelStatus,
            statusMessage = uiState.statusMessage,
        )
        SpeechStatusBar(
            speechStatus = uiState.speechStatus,
            speechStatusMessage = uiState.speechStatusMessage,
        )
        TtsStatusBar(
            ttsStatus = uiState.ttsStatus,
            ttsStatusMessage = uiState.ttsStatusMessage,
        )
        ChatToolbar(
            hasMessages = uiState.messages.isNotEmpty(),
            isGenerating = uiState.isGenerating,
            speechStatus = uiState.speechStatus,
            onClearMessages = viewModel::clearMessages,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    ttsStatus = uiState.ttsStatus,
                    onSpeak = viewModel::speakMessage,
                    onStopSpeaking = viewModel::stopSpeaking,
                )
            }
        }

        ChatInputBar(
            text = uiState.inputText,
            isGenerating = uiState.isGenerating,
            speechStatus = uiState.speechStatus,
            onTextChange = viewModel::onInputChange,
            onSend = viewModel::sendCurrentInput,
            onStartSpeech = ::requestSpeechInput,
            onStopSpeech = viewModel::stopSpeechInput,
        )
    }
}

@Composable
private fun ChatToolbar(
    hasMessages: Boolean,
    isGenerating: Boolean,
    speechStatus: SpeechStatus,
    onClearMessages: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onClearMessages,
                enabled = hasMessages && !isGenerating && speechStatus != SpeechStatus.LISTENING,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("清空")
            }
        }
    }
}

@Composable
private fun ModelStatusBar(
    modelStatus: ModelStatus,
    statusMessage: String,
) {
    val containerColor = when (modelStatus) {
        ModelStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        ModelStatus.LOADING -> MaterialTheme.colorScheme.secondaryContainer
        ModelStatus.READY -> MaterialTheme.colorScheme.primaryContainer
        ModelStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (modelStatus) {
        ModelStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        ModelStatus.LOADING -> MaterialTheme.colorScheme.onSecondaryContainer
        ModelStatus.READY -> MaterialTheme.colorScheme.onPrimaryContainer
        ModelStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 状态条用于区分首次加载、推理中、就绪和失败，方便真机调试。
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SpeechStatusBar(
    speechStatus: SpeechStatus,
    speechStatusMessage: String,
) {
    if (speechStatus == SpeechStatus.IDLE) return

    val containerColor = when (speechStatus) {
        SpeechStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        SpeechStatus.LISTENING -> MaterialTheme.colorScheme.tertiaryContainer
        SpeechStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (speechStatus) {
        SpeechStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        SpeechStatus.LISTENING -> MaterialTheme.colorScheme.onTertiaryContainer
        SpeechStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (speechStatus == SpeechStatus.LISTENING) "$speechStatusMessage 说完后点停止" else speechStatusMessage,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TtsStatusBar(
    ttsStatus: TtsStatus,
    ttsStatusMessage: String,
) {
    if (ttsStatus == TtsStatus.IDLE) return

    val containerColor = when (ttsStatus) {
        TtsStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        TtsStatus.SPEAKING -> MaterialTheme.colorScheme.secondaryContainer
        TtsStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (ttsStatus) {
        TtsStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        TtsStatus.SPEAKING -> MaterialTheme.colorScheme.onSecondaryContainer
        TtsStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = ttsStatusMessage,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    ttsStatus: TtsStatus,
    onSpeak: (ChatMessage) -> Unit,
    onStopSpeaking: () -> Unit,
) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth(0.86f),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "我" else "LiveSense",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (!isUser) {
                    TextButton(
                        onClick = {
                            if (ttsStatus == TtsStatus.SPEAKING) {
                                onStopSpeaking()
                            } else {
                                onSpeak(message)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(if (ttsStatus == TtsStatus.SPEAKING) "停止播放" else "播放日语")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    isGenerating: Boolean,
    speechStatus: SpeechStatus,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStartSpeech: () -> Unit,
    onStopSpeech: () -> Unit,
) {
    val isListening = speechStatus == SpeechStatus.LISTENING

    Surface(
        tonalElevation = 2.dp,
        color = Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入日语") },
                minLines = 1,
                maxLines = 4,
                enabled = !isGenerating && !isListening,
            )
            Button(
                onClick = if (isListening) onStopSpeech else onStartSpeech,
                enabled = !isGenerating,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(if (isListening) "停止" else "语音")
            }
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = onSend,
                    enabled = text.isNotBlank() && !isGenerating,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(if (isGenerating) "生成中" else "发送")
                }
            }
        }
    }
}
