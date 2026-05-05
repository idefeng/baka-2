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
            【用户原句意思】
            你好。
            【自然回应】
            こんにちは。
            【回应意思】
            你好。
            【语法分析】
            「こんにちは」是日常问候语。
        """.trimIndent()

        val formatted = LlmResponseFormatter.format(raw)

        assertEquals(
            "【用户原句意思】你好。\n\n【自然回应】こんにちは。\n\n【回应意思】你好。\n\n【语法分析】「こんにちは」是日常问候语。",
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

        assertTrue(formatted.contains("【用户原句意思】"))
        assertTrue(formatted.contains("【自然回应】"))
        assertTrue(formatted.contains("【回应意思】"))
        assertTrue(formatted.contains("【语法分析】"))
        assertFalse(formatted.contains("yo-yo-yo-yo-yo-yo-yo-yo"))
    }

    @Test
    fun format_whenOldThreeSectionResponseArrives_mapsCorrectionToGrammarAnalysis() {
        val raw = """
            【自然回应】こんにちは。
            【纠错】没有明显错误。
            【更自然表达】こんにちは。
        """.trimIndent()

        val formatted = LlmResponseFormatter.format(raw)

        assertEquals(
            "【用户原句意思】这句话是日常表达。\n\n【自然回应】こんにちは。\n\n【回应意思】这是自然的日语回应。\n\n【语法分析】没有明显错误。",
            formatted,
        )
    }
}
