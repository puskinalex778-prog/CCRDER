package com.nfcpay.simulator.domain.model

/**
 * Local demo history record.
 * Strictly stores ONLY: open non-requisite card description, amount, timestamp, and demo status.
 * NEVER stores PAN, masked PAN, expiry, CVV, Track 2, or bank receipt identifiers.
 */
data class DemoTransaction(
    val id: String,
    val amountKopecks: Long,
    val timestampMillis: Long,
    val status: String = STATUS_COMPLETED,
    val openCardInfo: String,
    val isNoCardDemo: Boolean = false
) {
    companion object {
        const val STATUS_COMPLETED = "Завершенные платежи"
    }
}
