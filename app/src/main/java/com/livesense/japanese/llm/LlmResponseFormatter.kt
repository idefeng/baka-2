package com.livesense.japanese.llm

object LlmResponseFormatter {
    private const val MAX_SECTION_LENGTH = 220

    fun format(rawResponse: String): String {
        val cleaned = rawResponse
            .replace("\r\n", "\n")
            .replace(Regex("-{3,}"), "")
            .trim()

        val userMeaning = extractSection(cleaned, "【用户原句意思】", listOf("【自然回应】"))
            ?: extractSection(cleaned, "【中文释义】", listOf("【自然回应】", "【用法场景】", "【纠错】"))
            ?: "这句话是日常表达。"
        val natural = extractSection(cleaned, "【自然回应】", listOf("【回应意思】", "【中文释义】", "【纠错】", "【语法分析】"))
            ?: cleaned.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val responseMeaning = extractSection(cleaned, "【回应意思】", listOf("【语法分析】", "【纠错】", "【更自然表达】"))
            ?: "这是自然的日语回应。"
        val grammar = extractSection(cleaned, "【语法分析】", emptyList())
            ?: extractSection(cleaned, "【纠错】", listOf("【更自然表达】"))
            ?: "没有明显语法错误。"

        return "【用户原句意思】${sanitizeSection(userMeaning, "这句话是日常表达。")}" +
            "\n\n【自然回应】${sanitizeSection(natural, "すみません、もう一度お願いします。")}" +
            "\n\n【回应意思】${sanitizeSection(responseMeaning, "这是自然的日语回应。")}" +
            "\n\n【语法分析】${sanitizeSection(grammar, "没有明显语法错误。")}"
    }

    private fun extractSection(text: String, startLabel: String, endLabels: List<String>): String? {
        val start = text.indexOf(startLabel)
        if (start < 0) return null

        val contentStart = start + startLabel.length
        val contentEnd = endLabels
            .mapNotNull { label -> text.indexOf(label, contentStart).takeIf { it >= 0 } }
            .minOrNull()
            ?: text.length
        return text.substring(contentStart, contentEnd).trim().ifEmpty { null }
    }

    private fun sanitizeSection(section: String, fallback: String): String {
        // 截断模型跑飞时常见的重复音节，避免 UI 被无意义长文本撑满。
        val withoutRunaway = section
            .replace(Regex("(?i)(yo-){6,}yo?"), "")
            .replace(Regex("(?i)(ha-){6,}ha?"), "")
            .replace(Regex("(?i)(n-){8,}n?"), "")
            .trim()

        return withoutRunaway
            .take(MAX_SECTION_LENGTH)
            .trim()
            .ifEmpty { fallback }
    }
}
