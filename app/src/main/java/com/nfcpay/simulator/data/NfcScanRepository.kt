package com.nfcpay.simulator.data

import android.content.Context
import com.nfcpay.simulator.domain.model.NdefRecordInfo
import com.nfcpay.simulator.domain.model.NfcScanRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

interface NfcScanRepository {
    val scans: StateFlow<List<NfcScanRecord>>
    fun saveScan(record: NfcScanRecord)
    fun deleteScan(id: String)
    fun clearScans()
}

/**
 * Local offline storage for saved public NFC tag/card technical scans.
 * Stores ONLY open metadata (title, note, UID, technologies, public EMV label/AIDs, NDEF records).
 */
class SharedPreferencesNfcScanRepository(context: Context) : NfcScanRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _scans = MutableStateFlow(load())
    override val scans: StateFlow<List<NfcScanRecord>> = _scans.asStateFlow()

    override fun saveScan(record: NfcScanRecord) {
        val filtered = _scans.value.filterNot { it.id == record.id }
        val updated = (listOf(record) + filtered).take(MAX_ITEMS)
        save(updated)
        _scans.value = updated
    }

    override fun deleteScan(id: String) {
        val updated = _scans.value.filterNot { it.id == id }
        save(updated)
        _scans.value = updated
    }

    override fun clearScans() {
        prefs.edit().remove(KEY_SCANS).apply()
        _scans.value = emptyList()
    }

    private fun load(): List<NfcScanRecord> {
        val raw = prefs.getString(KEY_SCANS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    if (id.isEmpty()) continue
                    val title = obj.optString("title", "NFC-метка")
                    val timestamp = obj.optLong("timestampMillis", 0L)
                    val uidHex = obj.optString("uidHex", "").ifEmpty { null }
                    val appLabel = obj.optString("applicationLabel", "").ifEmpty { null }
                    val techSummary = obj.optString("technicalSummary", "")
                    val note = obj.optString("note", "")

                    val techs = readStringArray(obj.optJSONArray("technologies"))
                    val aids = readStringArray(obj.optJSONArray("publicAids"))
                    val ndefs = readNdefArray(obj.optJSONArray("ndefRecords"))

                    add(
                        NfcScanRecord(
                            id = id,
                            title = title,
                            timestampMillis = timestamp,
                            uidHex = uidHex,
                            technologies = techs,
                            applicationLabel = appLabel,
                            publicAids = aids,
                            ndefRecords = ndefs,
                            technicalSummary = techSummary,
                            note = note
                        )
                    )
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun save(items: List<NfcScanRecord>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("timestampMillis", item.timestampMillis)
                put("uidHex", item.uidHex ?: "")
                put("applicationLabel", item.applicationLabel ?: "")
                put("technicalSummary", item.technicalSummary)
                put("note", item.note)
                put("technologies", JSONArray(item.technologies))
                put("publicAids", JSONArray(item.publicAids))
                val ndefArr = JSONArray()
                for (rec in item.ndefRecords) {
                    ndefArr.put(
                        JSONObject().apply {
                            put("recordType", rec.recordType)
                            put("content", rec.content)
                        }
                    )
                }
                put("ndefRecords", ndefArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SCANS, arr.toString()).apply()
    }

    private fun readStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val value = arr.optString(i, "")
                if (value.isNotEmpty()) add(value)
            }
        }
    }

    private fun readNdefArray(arr: JSONArray?): List<NdefRecordInfo> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val type = obj.optString("recordType", "")
                val content = obj.optString("content", "")
                if (type.isNotEmpty() || content.isNotEmpty()) {
                    add(NdefRecordInfo(recordType = type, content = content))
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "nfc_pay_scans"
        private const val KEY_SCANS = "saved_nfc_scans_v1"
        private const val MAX_ITEMS = 100
    }
}
