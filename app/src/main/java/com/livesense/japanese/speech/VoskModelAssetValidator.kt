package com.livesense.japanese.speech

object VoskModelAssetValidator {
    private val requiredFiles = listOf(
        "am/final.mdl",
        "conf/model.conf",
    )

    fun validate(relativePaths: Set<String>): String? {
        val missingFiles = requiredFiles.filterNot(relativePaths::contains)
        if (missingFiles.isEmpty()) return null

        // 这里提前指出缺失文件，避免 Vosk native 层只返回 failed to create a model。
        return "Vosk 日语模型目录不完整，请把完整模型解压到 app/src/main/assets/model/。缺少：${missingFiles.joinToString("、")}"
    }
}
