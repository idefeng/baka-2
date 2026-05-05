package com.livesense.japanese.speech

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoskModelAssetValidatorTest {
    @Test
    fun validate_whenOnlyUuidExists_returnsMissingModelMessage() {
        val result = VoskModelAssetValidator.validate(setOf("uuid"))

        assertTrue(result!!.contains("Vosk 日语模型目录不完整"))
        assertTrue(result.contains("am/final.mdl"))
        assertTrue(result.contains("conf/model.conf"))
    }

    @Test
    fun validate_whenRequiredModelFilesExist_returnsNull() {
        val result = VoskModelAssetValidator.validate(
            setOf(
                "uuid",
                "am/final.mdl",
                "conf/model.conf",
                "graph/HCLr.fst",
                "graph/Gr.fst",
            )
        )

        assertNull(result)
    }
}
