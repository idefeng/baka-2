package com.livesense.japanese.ui.chat

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livesense.japanese.data.ChatMessage
import com.livesense.japanese.data.MessageRole

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ModelStatusBar(
            modelStatus = uiState.modelStatus,
            statusMessage = uiState.statusMessage,
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
                ChatBubble(message = message)
            }
        }

        ChatInputBar(
            text = uiState.inputText,
            isGenerating = uiState.isGenerating,
            onTextChange = viewModel::onInputChange,
            onSend = viewModel::sendCurrentInput,
        )
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
private fun ChatBubble(message: ChatMessage) {
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
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    isGenerating: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
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
                enabled = !isGenerating,
            )
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
