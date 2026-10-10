package com.example.data

import android.content.Context
import com.example.core.ApiSettings
import com.example.security.KeystoreSecretManager

class SettingsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("sms_reader_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_URL = "api_url"
        private const val KEY_AUTH_TYPE = "auth_type"
        private const val KEY_AUTH_HEADER_NAME = "auth_header_name"
        private const val KEY_MERCHANT_ID = "merchant_id"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_TIMEOUT = "timeout_seconds"
        private const val KEY_PREVENT_DUPLICATES = "prevent_duplicates"
        private const val KEY_CONSENT_ACCEPTED = "initial_consent_accepted"
        private const val KEY_LANGUAGE = "app_language" // "en" or "bn"
    }

    fun getApiSettings(): ApiSettings {
        return ApiSettings(
            apiUrl = prefs.getString(KEY_API_URL, "https://api.merchant-pay.com/v1/reconcile") ?: "https://api.merchant-pay.com/v1/reconcile",
            authType = prefs.getString(KEY_AUTH_TYPE, "Bearer") ?: "Bearer",
            authHeaderName = prefs.getString(KEY_AUTH_HEADER_NAME, "Authorization") ?: "Authorization",
            merchantId = prefs.getString(KEY_MERCHANT_ID, "DEMO_MERCHANT_001") ?: "DEMO_MERCHANT_001",
            deviceId = prefs.getString(KEY_DEVICE_ID, "POS_TERMINAL_001") ?: "POS_TERMINAL_001",
            timeoutSeconds = prefs.getInt(KEY_TIMEOUT, 15),
            preventDuplicates = prefs.getBoolean(KEY_PREVENT_DUPLICATES, true)
        )
    }

    fun saveApiSettings(settings: ApiSettings, secret: String? = null) {
        prefs.edit()
            .putString(KEY_API_URL, settings.apiUrl)
            .putString(KEY_AUTH_TYPE, settings.authType)
            .putString(KEY_AUTH_HEADER_NAME, settings.authHeaderName)
            .putString(KEY_MERCHANT_ID, settings.merchantId)
            .putString(KEY_DEVICE_ID, settings.deviceId)
            .putInt(KEY_TIMEOUT, settings.timeoutSeconds)
            .putBoolean(KEY_PREVENT_DUPLICATES, settings.preventDuplicates)
            .apply()

        if (secret != null) {
            KeystoreSecretManager.storeApiSecret(context, secret)
        }
    }

    fun getApiSecret(): String {
        return KeystoreSecretManager.getApiSecret(context)
    }

    fun hasStoredSecret(): Boolean {
        return KeystoreSecretManager.hasStoredSecret(context)
    }

    fun isConsentAccepted(): Boolean {
        return prefs.getBoolean(KEY_CONSENT_ACCEPTED, false)
    }

    fun setConsentAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT_ACCEPTED, accepted).apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun clearAllSettings() {
        prefs.edit().clear().apply()
        KeystoreSecretManager.clearAllSecrets(context)
    }
}
