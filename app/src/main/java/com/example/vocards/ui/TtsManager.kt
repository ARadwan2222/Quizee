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
                applyLocaleForLanguage(currentLocale.language)
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

    fun getCandidateLocales(langInput: String): List<Locale> {
        val clean = langInput.lowercase().trim()
        return when (clean) {
            "arabic", "ar", "ar-sa" -> listOf(Locale.forLanguageTag("ar-SA"), Locale.forLanguageTag("ar-EG"), Locale.forLanguageTag("ar"))
            "spanish", "es", "es-es" -> listOf(Locale.forLanguageTag("es-ES"), Locale.forLanguageTag("es-US"), Locale.forLanguageTag("es-MX"), Locale.forLanguageTag("es"))
            "french", "fr", "fr-fr" -> listOf(Locale.forLanguageTag("fr-FR"), Locale.forLanguageTag("fr-CA"), Locale.forLanguageTag("fr"))
            "german", "de", "de-de" -> listOf(Locale.forLanguageTag("de-DE"), Locale.forLanguageTag("de"))
            "italian", "it", "it-it" -> listOf(Locale.forLanguageTag("it-IT"), Locale.forLanguageTag("it"))
            "chinese", "zh", "zh-cn" -> listOf(Locale.forLanguageTag("zh-CN"), Locale.forLanguageTag("zh-TW"), Locale.forLanguageTag("zh"))
            "japanese", "ja", "ja-jp" -> listOf(Locale.forLanguageTag("ja-JP"), Locale.forLanguageTag("ja"))
            "korean", "ko", "ko-kr" -> listOf(Locale.forLanguageTag("ko-KR"), Locale.forLanguageTag("ko"))
            "russian", "ru", "ru-ru" -> listOf(Locale.forLanguageTag("ru-RU"), Locale.forLanguageTag("ru"))
            "hindi", "hi", "hi-in" -> listOf(Locale.forLanguageTag("hi-IN"), Locale.forLanguageTag("hi"))
            "portuguese", "pt", "pt-pt" -> listOf(Locale.forLanguageTag("pt-PT"), Locale.forLanguageTag("pt-BR"), Locale.forLanguageTag("pt"))
            "turkish", "tr", "tr-tr" -> listOf(Locale.forLanguageTag("tr-TR"), Locale.forLanguageTag("tr"))
            "dutch", "nl" -> listOf(Locale.forLanguageTag("nl-NL"), Locale.forLanguageTag("nl"))
            "greek", "el" -> listOf(Locale.forLanguageTag("el-GR"), Locale.forLanguageTag("el"))
            "hebrew", "he" -> listOf(Locale.forLanguageTag("he-IL"), Locale.forLanguageTag("he"))
            "thai", "th" -> listOf(Locale.forLanguageTag("th-TH"), Locale.forLanguageTag("th"))
            "vietnamese", "vi" -> listOf(Locale.forLanguageTag("vi-VN"), Locale.forLanguageTag("vi"))
            "english", "en", "en-us" -> listOf(Locale.US, Locale.UK, Locale.forLanguageTag("en"))
            else -> listOf(Locale.forLanguageTag(clean))
        }
    }

    private fun applyLocaleForLanguage(langInput: String) {
        val candidates = getCandidateLocales(langInput)
        for (loc in candidates) {
            val res = tts?.setLanguage(loc)
            if (res != null && res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                return
            }
        }
        Log.w("TtsManager", "TTS engine missing voice data for language: $langInput")
    }

    fun setLanguage(langCode: String) {
        currentLocale = getCandidateLocales(langCode).firstOrNull() ?: Locale.US
        if (isInitialized) {
            applyLocaleForLanguage(langCode)
        }
    }

    fun speak(text: String, lang: String? = null) {
        if (isInitialized) {
            val detectedScriptLang = detectLanguageFromScript(text)
            val effectiveLang = detectedScriptLang ?: lang ?: currentLocale.language
            applyLocaleForLanguage(effectiveLang)
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
