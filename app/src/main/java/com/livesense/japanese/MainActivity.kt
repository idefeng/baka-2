package com.livesense.japanese

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.livesense.japanese.llm.AndroidMediaPipeLlmEngineFactory
import com.livesense.japanese.llm.MediaPipeLlmManager
import com.livesense.japanese.speech.VoskSpeechToTextManager
import com.livesense.japanese.ui.chat.ChatScreen
import com.livesense.japanese.ui.chat.ChatViewModel
import com.livesense.japanese.ui.theme.LiveSenseJapaneseTheme

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val llmManager = MediaPipeLlmManager(
                    engineFactory = AndroidMediaPipeLlmEngineFactory(applicationContext),
                )
                val speechToTextManager = VoskSpeechToTextManager(applicationContext)
                return ChatViewModel(llmManager, speechToTextManager) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LiveSenseJapaneseTheme {
                ChatScreen(viewModel = chatViewModel)
            }
        }
    }
}
