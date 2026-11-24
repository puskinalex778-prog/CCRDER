package com.nfcpay.simulator.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.os.Bundle
import com.nfcpay.simulator.domain.model.NfcHardwareStatus
import com.nfcpay.simulator.domain.model.NfcReadResult
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Controls Android NFC Reader Mode lifecycle strictly bound to the WaitingForCard screen.
 * Guarantees:
 * - Reader Mode is OFF until cardholder explicitly confirms consent.
 * - Single-shot AtomicBoolean guard prevents repeated taps from launching multiple demos.
 * - Automatically disables Reader Mode immediately after the first tag read or when stopped.
 */
class NfcReaderController(
    private val publicDataReader: EmvPublicDataReader = EmvPublicDataReader()
) {
    private val isActive = AtomicBoolean(false)
    private val singleReadConsumed = AtomicBoolean(false)

    fun checkHardwareStatus(activity: Activity): NfcHardwareStatus {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return NfcHardwareStatus.NOT_SUPPORTED
        return if (adapter.isEnabled) NfcHardwareStatus.ENABLED else NfcHardwareStatus.DISABLED
    }

    fun startReading(
        activity: Activity,
        onResult: (NfcReadResult) -> Unit
    ): NfcHardwareStatus {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
            ?: return NfcHardwareStatus.NOT_SUPPORTED
        if (!adapter.isEnabled) {
            return NfcHardwareStatus.DISABLED
        }

        singleReadConsumed.set(false)
        if (!isActive.compareAndSet(false, true)) {
            return NfcHardwareStatus.ENABLED
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val extras = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500)
        }

        val callback = NfcAdapter.ReaderCallback { tag ->
            // Guard against duplicate/rapid NFC callbacks triggering multiple demos
            if (!isActive.get() || !singleReadConsumed.compareAndSet(false, true)) {
                return@ReaderCallback
            }

            val result = publicDataReader.readTag(tag)

            // Stop Reader Mode immediately after one tag event
            activity.runOnUiThread {
                stopReading(activity)
                onResult(result)
            }
        }

        adapter.enableReaderMode(activity, callback, flags, extras)

        return NfcHardwareStatus.ENABLED
    }

    fun stopReading(activity: Activity) {
        if (!isActive.compareAndSet(true, false)) return
        singleReadConsumed.set(true)
        try {
            NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
        } catch (_: IllegalStateException) {
            // Activity already paused/destroyed
        }
    }
}
