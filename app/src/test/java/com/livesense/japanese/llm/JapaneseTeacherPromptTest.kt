package com.livesense.japanese.llm

import com.livesense.japanese.data.ChatMessage
import com.livesense.japanese.data.MessageRole
import com.livesense.japanese.prompt.JapaneseTeacherPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JapaneseTeacherPromptTest {
    @Test
    fun build_wrapsInputWithTeacherRulesExamplesAndContext() {
        val prompt = JapaneseTeacherPrompt.build(
            userInput = "私は学生です",
            recentMessages = listOf(
                ChatMessage(role = MessageRole.USER, content = "こんにちは"),
                ChatMessage(role = MessageRole.AI, content = "【自然回应】こんにちは。元気ですか。"),
            )
        )

        assertTrue(prompt.contains("你是一位严格遵守格式的日语老师。"))
        assertTrue(prompt.contains("【用户原句意思】"))
        assertTrue(prompt.contains("【自然回应】"))
        assertTrue(prompt.contains("【回应意思】"))
        assertTrue(prompt.contains("【语法分析】"))
        assertFalse(prompt.contains("【中文释义】"))
        assertFalse(prompt.contains("【用法场景】"))
        assertFalse(prompt.contains("\n        【纠错】"))
        assertFalse(prompt.contains("\n        【更自然表达】"))
        assertTrue(prompt.contains("示例1"))
        assertTrue(prompt.contains("最近对话："))
        assertTrue(prompt.contains("用户：こんにちは"))
        assertTrue(prompt.contains("老师：【自然回应】こんにちは。元気ですか。"))
        assertTrue(prompt.contains("用户原句意思、回应意思和语法分析必须使用中文"))
        assertTrue(prompt.contains("不要使用罗马音"))
        assertTrue(prompt.contains("用户输入：私は学生です"))
    }

    @Test
    fun build_guidesModelToExplainMeaningsAndAnalyzeUserGrammar() {
        val prompt = JapaneseTeacherPrompt.build("何も食べたくないです")

        assertTrue(prompt.contains("先理解用户原句的意思和情绪"))
        assertTrue(prompt.contains("不要把【自然回应】写成命令"))
        assertTrue(prompt.contains("【用户原句意思】解释用户说的日语是什么意思"))
        assertTrue(prompt.contains("【回应意思】解释【自然回应】是什么意思"))
        assertTrue(prompt.contains("【语法分析】对用户原句做简洁语法分析"))
        assertTrue(prompt.contains("何も食べたくないです"))
        assertTrue(prompt.contains("我什么也不想吃"))
        assertFalse(prompt.contains("はい、食べないでください"))
    }
}
