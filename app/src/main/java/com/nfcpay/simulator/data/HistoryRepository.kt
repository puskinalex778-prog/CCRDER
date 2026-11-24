package com.nfcpay.simulator.data

import android.content.Context
import com.nfcpay.simulator.domain.model.DemoTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

interface HistoryRepository {
    val history: StateFlow<List<DemoTransaction>>
    fun addTransaction(transaction: DemoTransaction)
    fun clearHistory()
}

/**
 * Local-only offline history store.
 * Persists ONLY non-sensitive demo metadata:
 * - amountKopecks
 * - timestampMillis
 * - status ("Завершенные платежи")
 * - openCardInfo (public card tech / EMV label or "Без считывания карты")
 * - isNoCardDemo
 *
 * NEVER persists card numbers (full or masked), expiry dates, CVV, Track 2, or bank receipt IDs.
 */
class SharedPreferencesHistoryRepository(context: Context) : HistoryRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(load())
    override val history: StateFlow<List<DemoTransaction>> = _history.asStateFlow()

    override fun addTransaction(transaction: DemoTransaction) {
        val updated = (listOf(transaction) + _history.value).take(MAX_ITEMS)
        save(updated)
        _history.value = updated
    }

    override fun clearHistory() {
        prefs.edit().remove(KEY_TRANSACTIONS).apply()
        _history.value = emptyList()
    }

    private fun load(): List<DemoTransaction> {
        val raw = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "").ifEmpty { continue }
                    val amount = obj.optLong("amountKopecks", 0L)
                    val timestamp = obj.optLong("timestampMillis", 0L)
                    val status = obj.optString("status", DemoTransaction.STATUS_COMPLETED)
                    val openCardInfo = obj.optString("openCardInfo", "Без считывания карты")
                    val isNoCardDemo = obj.optBoolean("isNoCardDemo", false)
                    if (amount > 0L && timestamp > 0L) {
                        add(
                            DemoTransaction(
                                id = id,
                                amountKopecks = amount,
                                timestampMillis = timestamp,
                                status = status,
                                openCardInfo = openCardInfo,
                                isNoCardDemo = isNoCardDemo
                            )
                        )
                    }
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun save(items: List<DemoTransaction>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("amountKopecks", item.amountKopecks)
                put("timestampMillis", item.timestampMillis)
                put("status", item.status)
                put("openCardInfo", item.openCardInfo)
                put("isNoCardDemo", item.isNoCardDemo)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "nfc_pay_history"
        private const val KEY_TRANSACTIONS = "demo_transactions_v1"
        private const val MAX_ITEMS = 100
    }
}
