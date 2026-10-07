package com.mosman.wird

import com.mosman.wird.audio.RecitationModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recitation models are pinned to one published version and fingerprinted (2026-10-04).
 * Before, the app fetched whatever the repository's "main" held and trusted any file over 10 MB.
 */
class RecitationModelTest {

    @Test
    fun every_model_names_one_version_and_its_fingerprint() {
        for (model in RecitationModel.entries) {
            assertFalse("${model.name} follows main", model.url.contains("/resolve/main/"))
            assertTrue("${model.name} is pinned", model.url.contains("/resolve/4a96d8bb5535a4b6e6f6abd5655b8711a95ae538/"))
            assertTrue("${model.name} SHA-256", Regex("[0-9a-f]{64}").matches(model.sha256))
        }
    }

    @Test
    fun the_sizes_shown_match_the_files() {
        for (model in RecitationModel.entries) {
            assertEquals(model.name, model.megabytes.toLong(), Math.round(model.bytes / (1024.0 * 1024.0)))
        }
    }
}
