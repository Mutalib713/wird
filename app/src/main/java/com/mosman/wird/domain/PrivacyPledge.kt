package com.mosman.wird.domain

/**
 * The Wird Privacy Pledge.
 *
 * Sacred Rule 1: All user data, recordings, history, and reading position stay 100%
 * on the user's phone. No remote servers, no cloud sync, no tracking, no analytics.
 *
 * Plain words first:
 * Your wird is an act of worship between you and your Creator. The app does not need
 * to know who you are, what you read, or how you sound. Everything stays strictly inside
 * this device.
 */
object PrivacyPledge {

    const val TITLE = "Privacy Pledge"
    const val SUBTITLE = "100% on-device · No tracking, no cloud upload"
    const val PROMISE = "Your wird is between you and your Creator."

    data class PledgeItem(
        val title: String,
        val description: String,
    )

    val ITEMS = listOf(
        PledgeItem(
            title = "Voice recordings stay on this device",
            description = "Audio from your recitation is processed entirely on your phone using offline speech recognition. Your voice never leaves this device and is never uploaded to any server or cloud API.",
        ),
        PledgeItem(
            title = "Zero telemetry and zero tracking",
            description = "Wird contains no analytics SDKs, no advertising identifiers, no crash telemetry phoning home, and no user profiling. We do not inspect, collect, or monetize your activity.",
        ),
        PledgeItem(
            title = "No accounts or remote database",
            description = "No email address, password, or sign-in is ever required. Your reading position, daily streaks, and bookmarks live only in this phone's private storage.",
        ),
        PledgeItem(
            title = "Full data sovereignty",
            description = "You own every byte of your data. You can export everything as a standard zip archive at any time, or clear all audio recordings with a single tap.",
        ),
        PledgeItem(
            title = "Audio recitations are direct & private",
            description = "Imam recitations stream directly from Quran.com or can be saved locally for full offline listening. No intermediary proxy inspects what you listen to.",
        ),
    )

    /**
     * Verifies that the pledge text contains no empty items or blank descriptions.
     */
    fun verifyIntegrity(): Boolean {
        return ITEMS.isNotEmpty() &&
            ITEMS.all { it.title.isNotBlank() && it.description.isNotBlank() }
    }
}
