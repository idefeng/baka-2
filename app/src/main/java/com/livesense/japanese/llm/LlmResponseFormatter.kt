package com.livesense.japanese.llm

object LlmResponseFormatter {
    private const val MAX_SECTION_LENGTH = 220

    fun format(rawResponse: String): String {
        val cleaned = rawResponse
            .replace("\r\n", "\n")
            .replace(Regex("-{3,}"), "")
            .trim()

        val natural = extractSection(cleaned, "【自然回应】", "【中文释义】")
            ?: cleaned.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val meaning = extractSection(cleaned, "【中文释义】", "【用法场景】")
            ?: "这句话是日常表达，表示打招呼或回应。"
        val usage = extractSection(cleaned, "【用法场景】", "【纠错】")
            ?: "适合日常口语，语气自然；和熟人、店员、老师都可以使用。"
        val correction = extractSection(cleaned, "【纠错】", "【更自然表达】")
            ?: "没有明显错误。注意根据场合选择更正式或更口语的说法。"
        val better = extractSection(cleaned, "【更自然表达】", null)
            ?: natural

        return "【自然回应】${sanitizeSection(natural)}" +
            "\n\n【中文释义】${sanitizeSection(meaning)}" +
            "\n\n【用法场景】${sanitizeSection(usage)}" +
            "\n\n【纠错】${sanitizeSection(correction)}" +
            "\n\n【更自然表达】${sanitizeSection(better)}"
    }

    private fun extractSection(text: String, startLabel: String, endLabel: String?): String? {
        val start = text.indexOf(startLabel)
        if (start < 0) return null

        val contentStart = start + startLabel.length
        val contentEnd = if (endLabel == null) {
            text.length
        } else {
            text.indexOf(endLabel, contentStart).takeIf { it >= 0 } ?: text.length
        }
        return text.substring(contentStart, contentEnd).trim().ifEmpty { null }
    }

    private fun sanitizeSection(section: String): String {
        // 截断模型跑飞时常见的重复音节，避免 UI 被无意义长文本撑满。
        val withoutRunaway = section
            .replace(Regex("(?i)(yo-){6,}yo?"), "")
            .replace(Regex("(?i)(ha-){6,}ha?"), "")
            .replace(Regex("(?i)(n-){8,}n?"), "")
            .trim()

        return withoutRunaway
            .take(MAX_SECTION_LENGTH)
            .trim()
            .ifEmpty { "没有明显错误。" }
    }
}
