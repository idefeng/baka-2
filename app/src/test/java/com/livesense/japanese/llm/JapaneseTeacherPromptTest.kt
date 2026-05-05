package com.livesense.japanese.llm

import com.livesense.japanese.prompt.JapaneseTeacherPrompt
import org.junit.Assert.assertTrue
import org.junit.Test

class JapaneseTeacherPromptTest {
    @Test
    fun build_wrapsInputWithTeacherRulesAndRequiredSections() {
        val prompt = JapaneseTeacherPrompt.build("私は学生です")

        assertTrue(prompt.contains("你是一位严格遵守格式的日语老师。"))
        assertTrue(prompt.contains("【自然回应】"))
        assertTrue(prompt.contains("【中文释义】"))
        assertTrue(prompt.contains("【用法场景】"))
        assertTrue(prompt.contains("【纠错】"))
        assertTrue(prompt.contains("【更自然表达】"))
        assertTrue(prompt.contains("中文释义、用法场景、纠错部分必须使用中文"))
        assertTrue(prompt.contains("不要使用罗马音"))
        assertTrue(prompt.contains("用户输入：私は学生です"))
    }
}
