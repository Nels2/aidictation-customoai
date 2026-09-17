package com.whispermate.aidictation.data.preferences

import com.whispermate.aidictation.BuildConfig
import android.content.Context
import com.whispermate.aidictation.data.local.ParakeetRuntime
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

enum class ApiProvider {
    PARAKEET,
    WRITINGMATE,
    OPENAI,
    GROQ,
    CUSTOM_OPENAI;

    fun transcriptionEndpoint(): String = when (this) {
        PARAKEET -> ""
        WRITINGMATE -> "https://writingmate.ai/api/openai/v1/audio/transcriptions"
        OPENAI -> "https://api.openai.com/v1/audio/transcriptions"
        GROQ -> "https://api.groq.com/openai/v1/audio/transcriptions"
        CUSTOM_OPENAI -> ""
    }

    fun llmEndpoint(): String = when (this) {
        PARAKEET -> ""
        WRITINGMATE -> "https://writingmate.ai/api/openai/v1/chat/completions"
        OPENAI -> "https://api.openai.com/v1/chat/completions"
        GROQ -> "https://api.groq.com/openai/v1/chat/completions"
        CUSTOM_OPENAI -> ""
    }
}

data class ApiConfig(
    val provider: ApiProvider,
    val apiKey: String,
    val model: String,
    val endpoint: String
)

// Runtime API configuration mirrors the macOS bundled AIDictation provider.
// Android no longer exposes provider/model/key editing, so stale local UI
// preferences cannot change which transcription endpoint or model is used.
@Singleton
class ApiConfigManager @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        @Volatile
        var instance: ApiConfigManager? = null
            private set

        fun defaultTranscriptionModel(provider: ApiProvider): String = when (provider) {
            ApiProvider.PARAKEET -> when (ParakeetRuntime.fromConfig(BuildConfig.PARAKEET_RUNTIME)) {
                ParakeetRuntime.ONNX -> "parakeet-tdt-0.6b-v3"
                ParakeetRuntime.LITERT -> "parakeet-tdt-0.6b-v3-litert"
            }
            ApiProvider.WRITINGMATE -> "soniox/stt-async-v5"
            ApiProvider.OPENAI -> "gpt-transcribe"
            ApiProvider.GROQ -> "whisper-large-v3-turbo"
            ApiProvider.CUSTOM_OPENAI -> ""
        }

        fun defaultPostProcessingModel(): String = "openai/gpt-oss-20b"
    }

    private var transcriptionConfig = buildCloudTranscriptionConfig()
    private var postProcessingConfig = buildDefaultPostProcessingConfig()

    init {
        instance = this
    }

    fun getTranscriptionConfig(): ApiConfig = transcriptionConfig

    fun switchTranscriptionToCloud() {
        transcriptionConfig = buildCloudTranscriptionConfig()
    }

    /** Explicit opt-in; defaults alone never change the selected cloud route. */
    fun switchTranscriptionToCustomServer() {
        transcriptionConfig = buildCustomServerConfig()
        postProcessingConfig = buildCustomCleanupConfig()
    }

    fun getPostProcessingConfig(): ApiConfig = postProcessingConfig

    private fun defaultTranscriptionProvider(): ApiProvider {
        val endpoint = BuildConfig.TRANSCRIPTION_ENDPOINT
        return when {
            endpoint.contains("writingmate", ignoreCase = true) -> ApiProvider.WRITINGMATE
            endpoint.contains("groq", ignoreCase = true) -> ApiProvider.GROQ
            endpoint.contains("openai", ignoreCase = true) -> ApiProvider.OPENAI
            else -> ApiProvider.WRITINGMATE
        }
    }

    private fun defaultPostProcessingProvider(): ApiProvider {
        val endpoint = BuildConfig.AIDICTATION_POST_PROCESSING_ENDPOINT
        return when {
            endpoint.contains("writingmate", ignoreCase = true) -> ApiProvider.WRITINGMATE
            endpoint.contains("groq", ignoreCase = true) -> ApiProvider.GROQ
            endpoint.contains("openai", ignoreCase = true) -> ApiProvider.OPENAI
            else -> ApiProvider.WRITINGMATE
        }
    }

    private fun buildCloudTranscriptionConfig(): ApiConfig {
        val provider = defaultTranscriptionProvider()
        return ApiConfig(
            provider = provider,
            apiKey = BuildConfig.TRANSCRIPTION_API_KEY,
            model = BuildConfig.TRANSCRIPTION_MODEL.ifEmpty { defaultTranscriptionModel(provider) },
            endpoint = BuildConfig.TRANSCRIPTION_ENDPOINT.ifEmpty { provider.transcriptionEndpoint() }
        )
    }

    private fun buildDefaultPostProcessingConfig(): ApiConfig {
        val provider = defaultPostProcessingProvider()
        return ApiConfig(
            provider = provider,
            apiKey = BuildConfig.AIDICTATION_POST_PROCESSING_KEY,
            model = BuildConfig.AIDICTATION_POST_PROCESSING_MODEL.ifEmpty { defaultPostProcessingModel() },
            endpoint = BuildConfig.AIDICTATION_POST_PROCESSING_ENDPOINT.ifEmpty { provider.llmEndpoint() }
        )
    }

    private fun buildCustomServerConfig(): ApiConfig {
        val profile = CustomOpenAIProfile.load(context)
        return ApiConfig(
            provider = ApiProvider.CUSTOM_OPENAI,
            apiKey = CustomOpenAICredentialStore.transcriptionKey(context),
            model = profile.transcriptionModel,
            endpoint = profile.transcriptionEndpoint().orEmpty()
        )
    }

    private fun buildCustomCleanupConfig(): ApiConfig {
        val profile = CustomOpenAIProfile.load(context)
        return ApiConfig(
            provider = ApiProvider.CUSTOM_OPENAI,
            apiKey = CustomOpenAICredentialStore.cleanupKey(context),
            model = profile.cleanupModel,
            endpoint = profile.cleanupEndpoint().orEmpty()
        )
    }
}
