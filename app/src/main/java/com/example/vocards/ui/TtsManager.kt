package com.example.vocards.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: Pair<String, String?>? = null
    private var currentLocale: Locale = Locale.US

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = currentLocale
                pendingText?.let { (text, lang) ->
                    speak(text, lang)
                    pendingText = null
                }
            } else {
                Log.e("TtsManager", "Initialization failed")
            }
        }
    }

    fun getLocaleForLanguage(lang: String?): Locale {
        if (lang.isNullOrBlank()) return currentLocale
        val clean = lang.trim()
        val code = when (clean.lowercase()) {
            "arabic", "ar", "ar-sa" -> "ar"
            "bengali", "bn", "bn-bd" -> "bn"
            "chinese", "zh", "zh-cn" -> "zh"
            "dutch", "nl" -> "nl"
            "english", "en", "en-us", "en-gb" -> "en"
            "french", "fr", "fr-fr" -> "fr"
            "german", "de", "de-de" -> "de"
            "greek", "el" -> "el"
            "hebrew", "he" -> "he"
            "hindi", "hi", "hi-in" -> "hi"
            "indonesian", "id" -> "id"
            "italian", "it", "it-it" -> "it"
            "japanese", "ja", "ja-jp" -> "ja"
            "korean", "ko", "ko-kr" -> "ko"
            "malay", "ms" -> "ms"
            "marathi", "mr", "mr-in" -> "mr"
            "persian", "fa" -> "fa"
            "polish", "pl" -> "pl"
            "portuguese", "pt", "pt-pt" -> "pt"
            "punjabi", "pa", "pa-in" -> "pa"
            "russian", "ru", "ru-ru" -> "ru"
            "spanish", "es", "es-es" -> "es"
            "swedish", "sv" -> "sv"
            "tamil", "ta", "ta-in" -> "ta"
            "telugu", "te", "te-in" -> "te"
            "thai", "th" -> "th"
            "turkish", "tr", "tr-tr" -> "tr"
            "ukrainian", "uk" -> "uk"
            "urdu", "ur", "ur-pk" -> "ur"
            "vietnamese", "vi", "vi-vn" -> "vi"
            else -> if (clean.contains("-") || clean.length == 2) clean else "en"
        }
        return Locale.forLanguageTag(code)
    }

    fun setLanguage(langCode: String) {
        currentLocale = getLocaleForLanguage(langCode)
        if (isInitialized) {
            tts?.language = currentLocale
        }
    }

    fun speak(text: String, lang: String? = null) {
        if (isInitialized) {
            val locale = if (!lang.isNullOrBlank()) getLocaleForLanguage(lang) else currentLocale
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VocardsTTS")
        } else {
            pendingText = Pair(text, lang)
        }
    }

    fun shutDown() {
        tts?.stop()
        tts?.shutdown()
    }
}
