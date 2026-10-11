package com.taskpie.app.service

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceAnnouncementManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            try {
                val localeEs = Locale("es", "ES")
                val langResult = tts?.setLanguage(localeEs)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val genericEs = Locale("es")
                    val genericResult = tts?.setLanguage(genericEs)
                    if (genericResult == TextToSpeech.LANG_MISSING_DATA || genericResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = Locale.getDefault()
                    }
                }

                tts?.setSpeechRate(1.05f)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)

                isInitialized = true

                pendingText?.let { text ->
                    speak(text)
                    pendingText = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring TextToSpeech attributes/language", e)
            }
        } else {
            Log.w(TAG, "TextToSpeech initialization returned status: $status")
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) {
            pendingText = text
            return
        }
        try {
            tts?.speak(text, queueMode, null, "UTTERANCE_${System.currentTimeMillis()}")
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
