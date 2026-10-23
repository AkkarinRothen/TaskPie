package com.taskpie.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceAnnouncementManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        initTts()
    }

    private fun initTts() {
        try {
            // First check if Google TTS is installed as preferred high-quality engine
            val appContext = context.applicationContext
            var engineName: String? = null
            try {
                val tempTts = TextToSpeech(appContext, null)
                val engines = tempTts.engines
                tempTts.shutdown()
                val hasGoogleTts = engines.any { it.name == "com.google.android.tts" }
                if (hasGoogleTts) {
                    engineName = "com.google.android.tts"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking engines, using default", e)
            }

            tts = if (engineName != null) {
                Log.d(TAG, "Initializing TextToSpeech with engine: $engineName")
                TextToSpeech(appContext, this, engineName)
            } else {
                Log.d(TAG, "Initializing TextToSpeech with system default engine")
                TextToSpeech(appContext, this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            try {
                // Find the best available Spanish locale
                val defaultLoc = Locale.getDefault()
                val candidateLocales = listOf(
                    defaultLoc,
                    Locale("es", "US"),
                    Locale("es", "ES"),
                    Locale("es", "MX"),
                    Locale("es", "AR"),
                    Locale("es")
                )

                var matchedLocale: Locale? = null
                for (loc in candidateLocales) {
                    val availability = tts?.isLanguageAvailable(loc) ?: TextToSpeech.LANG_NOT_SUPPORTED
                    Log.d(TAG, "Checking locale ${loc.toLanguageTag()}: availability = $availability")
                    if (availability >= TextToSpeech.LANG_AVAILABLE) {
                        matchedLocale = loc
                        break
                    }
                }

                val finalLocale = matchedLocale ?: Locale("es", "ES")
                val langResult = tts?.setLanguage(finalLocale)
                Log.d(TAG, "Selected TTS language: ${finalLocale.toLanguageTag()}, result = $langResult")

                tts?.setSpeechRate(1.02f)
                tts?.setPitch(1.0f)

                // Critical: Use USAGE_MEDIA so audio goes through STREAM_MUSIC (Media volume slider)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS Utterance onStart: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS Utterance onDone: $utteranceId")
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS Utterance onError: $utteranceId")
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(TAG, "TTS Utterance onError: $utteranceId, errorCode: $errorCode")
                    }
                })

                isInitialized = true
                Log.i(TAG, "TextToSpeech successfully initialized and ready!")

                pendingText?.let { text ->
                    speak(text)
                    pendingText = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring TextToSpeech attributes/language", e)
            }
        } else {
            Log.w(TAG, "TextToSpeech initialization failed with status: $status")
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) {
            Log.d(TAG, "TTS not yet initialized, buffering speech: '$text'")
            pendingText = text
            return
        }
        try {
            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            val utteranceId = "TP_${System.currentTimeMillis()}"
            val res = tts?.speak(text, queueMode, params, utteranceId)
            Log.d(TAG, "Speaking: '$text' (utteranceId=$utteranceId, result=$res)")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: $text", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
            pendingText = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        } finally {
            tts = null
            isInitialized = false
            pendingText = null
        }
    }

    companion object {
        private const val TAG = "VoiceAnnouncementMgr"
    }
}
