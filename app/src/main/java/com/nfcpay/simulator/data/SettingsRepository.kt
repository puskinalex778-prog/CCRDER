package com.nfcpay.simulator.data

import android.content.Context
import com.nfcpay.simulator.domain.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun setSoundEnabled(enabled: Boolean)
    fun setVibrationEnabled(enabled: Boolean)
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _settings.value = _settings.value.copy(soundEnabled = enabled)
    }

    override fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _settings.value = _settings.value.copy(vibrationEnabled = enabled)
    }

    private fun load(): AppSettings = AppSettings(
        soundEnabled = prefs.getBoolean(KEY_SOUND, true),
        vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true)
    )

    companion object {
        private const val PREFS_NAME = "nfc_pay_settings"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
    }
}
