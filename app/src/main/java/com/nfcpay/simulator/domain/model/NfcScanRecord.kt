package com.nfcpay.simulator.domain.model

/**
 * Saved open technical NFC scan record.
 * Stores ONLY public non-sensitive NFC metadata (UID, technologies, NDEF text/URI, public EMV label/AID).
 * NEVER extracts or stores bank card numbers (PAN), expiry dates, CVV, or Track 2 data.
 */
data class NfcScanRecord(
    val id: String,
    val title: String,
    val timestampMillis: Long,
    val uidHex: String? = null,
    val technologies: List<String> = emptyList(),
    val applicationLabel: String? = null,
    val publicAids: List<String> = emptyList(),
    val ndefRecords: List<NdefRecordInfo> = emptyList(),
    val technicalSummary: String = "",
    val note: String = ""
)

data class NdefRecordInfo(
    val recordType: String,
    val content: String
)

sealed interface NfcScanReadResult {
    data class Scanned(val record: NfcScanRecord) : NfcScanReadResult
    data object UnsupportedTag : NfcScanReadResult
    data object ReadInterrupted : NfcScanReadResult
}
