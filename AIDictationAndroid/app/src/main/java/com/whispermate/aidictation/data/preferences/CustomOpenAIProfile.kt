package com.whispermate.aidictation.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.io.File
import java.net.URI

/** Non-secret defaults. App-owned preferences win; this file is never rewritten. */
data class CustomOpenAIProfile(
    val transcriptionBaseUrl: String = "",
    val transcriptionModel: String = "",
    val realtimeUrl: String = "",
    val realtimeModel: String = "",
    val cleanupBaseUrl: String = "",
    val cleanupModel: String = ""
) {
    fun transcriptionEndpoint(): String? = derive(transcriptionBaseUrl, "/audio/transcriptions")
    fun cleanupEndpoint(): String? = derive(cleanupBaseUrl, "/chat/completions")

    companion object {
        fun load(context: Context): CustomOpenAIProfile = runCatching {
            val root = JSONObject(File(context.filesDir, "custom-openai.json").readText())
            val transcription = root.optJSONObject("transcription") ?: JSONObject()
            val cleanup = root.optJSONObject("cleanup") ?: JSONObject()
            CustomOpenAIProfile(
                transcription.optString("baseUrl"), transcription.optString("model"),
                transcription.optString("realtimeUrl"), transcription.optString("realtimeModel"),
                cleanup.optString("baseUrl"), cleanup.optString("model")
            )
        }.getOrDefault(CustomOpenAIProfile())

        fun derive(baseUrl: String, suffix: String): String? = runCatching {
            val uri = URI(baseUrl.trim().trimEnd('/'))
            val local = uri.host.equals("localhost", true) || uri.host == "127.0.0.1" || uri.host == "::1"
            require(uri.scheme == "https" || (local && uri.scheme == "http"))
            uri.toString().trimEnd('/') + suffix
        }.getOrNull()

        fun allowedRealtime(value: String): Boolean = runCatching {
            val uri = URI(value)
            val local = uri.host.equals("localhost", true) || uri.host == "127.0.0.1" || uri.host == "::1"
            uri.scheme == "wss" || (local && uri.scheme == "ws")
        }.getOrDefault(false)
    }
}

/** Separate secure credentials for the two independently configurable routes. */
object CustomOpenAICredentialStore {
    private const val FILE = "custom_openai_credentials"
    private const val TRANSCRIPTION = "transcription_key"
    private const val CLEANUP = "cleanup_key"

    private fun preferences(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun transcriptionKey(context: Context): String = preferences(context).getString(TRANSCRIPTION, "").orEmpty()
    fun cleanupKey(context: Context): String = preferences(context).getString(CLEANUP, "").orEmpty()
    fun save(context: Context, transcription: String, cleanup: String) {
        preferences(context).edit().putString(TRANSCRIPTION, transcription).putString(CLEANUP, cleanup).apply()
    }
}
