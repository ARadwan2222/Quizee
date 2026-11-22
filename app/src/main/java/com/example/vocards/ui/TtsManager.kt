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
                applyLocale(currentLocale)
                pendingText?.let { (text, lang) ->
                    speak(text, lang)
                    pendingText = null
                }
            } else {
                Log.e("TtsManager", "Initialization failed")
            }
        }
    }

    private fun detectLanguageFromScript(text: String): String? {
        var hasArabic = false
        var hasCyrillic = false
        var hasDevanagari = false
        var hasJapanese = false
        var hasKorean = false
        var hasChinese = false
        var hasHebrew = false

        for (ch in text) {
            val code = ch.code
            when {
                code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0x08A0..0x08FF -> hasArabic = true
                code in 0x0400..0x04FF -> hasCyrillic = true
                code in 0x0900..0x097F -> hasDevanagari = true
                code in 0x3040..0x30FF -> hasJapanese = true
                code in 0xAC00..0xD7AF -> hasKorean = true
                code in 0x4E00..0x9FFF -> hasChinese = true
                code in 0x0590..0x05FF -> hasHebrew = true
            }
        }

        return when {
            hasArabic -> "ar"
            hasJapanese -> "ja"
            hasKorean -> "ko"
            hasChinese -> "zh"
            hasDevanagari -> "hi"
            hasCyrillic -> "ru"
            hasHebrew -> "he"
            else -> null
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
            else -> if (clean.contains("-")) clean.substringBefore("-") else clean
        }
        return Locale.forLanguageTag(code)
    }

    private fun applyLocale(locale: Locale) {
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            val fallback = Locale.forLanguageTag(locale.language)
            val res2 = tts?.setLanguage(fallback)
            if (res2 == TextToSpeech.LANG_MISSING_DATA || res2 == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "TTS voice data for ${locale.language} is missing or unsupported on device.")
            }
        }
    }

    fun setLanguage(langCode: String) {
        currentLocale = getLocaleForLanguage(langCode)
        if (isInitialized) {
            applyLocale(currentLocale)
        }
    }

    fun speak(text: String, lang: String? = null) {
        if (isInitialized) {
            val detectedScriptLang = detectLanguageFromScript(text)
            val effectiveLang = detectedScriptLang ?: lang
            val locale = if (!effectiveLang.isNullOrBlank()) getLocaleForLanguage(effectiveLang) else currentLocale
            applyLocale(locale)
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
