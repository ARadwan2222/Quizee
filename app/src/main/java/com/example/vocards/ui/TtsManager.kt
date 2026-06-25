package com.example.vocards.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null
    private var currentLocale: Locale = Locale.US

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setLanguage(currentLocale.language)
                pendingText?.let { 
                    speak(it)
                    pendingText = null
                }
            } else {
                Log.e("TtsManager", "Initialization failed")
            }
        }
    }

    fun setLanguage(langCode: String) {
        currentLocale = when (langCode.lowercase()) {
            "ar" -> Locale("ar")
            "en_gb" -> Locale.UK
            "fr" -> Locale.FRANCE
            "de" -> Locale.GERMANY
            "es" -> Locale("es")
            else -> Locale.US
        }
        tts?.language = currentLocale
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VocardsTTS")
        } else {
            pendingText = text
        }
    }

    fun shutDown() {
        tts?.stop()
        tts?.shutdown()
    }
}
