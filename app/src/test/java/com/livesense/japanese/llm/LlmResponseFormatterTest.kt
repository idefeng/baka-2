package com.livesense.japanese.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmResponseFormatterTest {
    @Test
    fun format_keepsRequiredSectionsAndRemovesPreamble() {
        val raw = """
            ---
            こんにちは！余計な前置き
            【自然回应】
            こんにちは。
            【中文释义】
            你好。
            【用法场景】
            见面打招呼时使用。
            【纠错】
            没有明显错误。
            【更自然表达】
            こんにちは。
            追加の長い説明
        """.trimIndent()

        val formatted = LlmResponseFormatter.format(raw)

        assertEquals(
            "【自然回应】こんにちは。\n\n【中文释义】你好。\n\n【用法场景】见面打招呼时使用。\n\n【纠错】没有明显错误。\n\n【更自然表达】こんにちは。\n追加の長い説明",
            formatted,
        )
    }

    @Test
    fun format_truncatesRunawayRepetition() {
        val raw = """
            はい、いいですよ！
            yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo-yo
        """.trimIndent()

        val formatted = LlmResponseFormatter.format(raw)

        assertTrue(formatted.contains("【自然回应】"))
        assertTrue(formatted.contains("【中文释义】"))
        assertTrue(formatted.contains("【用法场景】"))
        assertTrue(formatted.contains("【纠错】"))
        assertTrue(formatted.contains("【更自然表达】"))
        assertFalse(formatted.contains("yo-yo-yo-yo-yo-yo-yo-yo"))
    }
}
