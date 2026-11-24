package com.nfcpay.simulator.domain.model

/**
 * Holds ONLY non-sensitive public card info for transient UI display.
 * Full PAN, expiry date, Track 2, CVV, and cardholder credentials are NEVER stored here.
 */
data class CardPublicMetadata(
    val last4Digits: String? = null,
    val applicationLabel: String? = null,
    val nfcTechnology: String = "",
    val isNoCardDemo: Boolean = false
) {
    val hasVerifiedLast4: Boolean
        get() = !isNoCardDemo &&
            last4Digits != null &&
            last4Digits.length == 4 &&
            last4Digits.all { it.isDigit() }

    /**
     * Public open description safe for local history without any card number or sensitive requisites.
     */
    fun toOpenHistoryDescription(): String {
        if (isNoCardDemo) {
            return "Без считывания карты"
        }
        val cleanLabel = applicationLabel?.trim().takeIf { !it.isNullOrEmpty() }
        val cleanTech = nfcTechnology.trim().takeIf { it.isNotEmpty() } ?: "NFC"
        return when {
            cleanLabel != null -> "Бесконтактная карта · $cleanLabel ($cleanTech)"
            else -> "Бесконтактная карта · $cleanTech"
        }
    }

    companion object {
        fun noCardDemo(): CardPublicMetadata = CardPublicMetadata(
            last4Digits = null,
            applicationLabel = null,
            nfcTechnology = "Демо без NFC",
            isNoCardDemo = true
        )
    }
}
