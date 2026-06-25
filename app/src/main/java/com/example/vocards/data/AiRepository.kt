package com.example.vocards.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class AiRepository(private var apiKey: String) {
    private val SYSTEM_KEY = "AIzaSyB4jSqfKUXCtQNyUxQ7xMLB3GcPVad49gI" 

    // --- المكتبة المحلية (Fallback Library) ---
    private val localTrivia = listOf(
        Vocad(0, -1, "What is the capital of France?", "Paris", "Paris is the city of light."),
        Vocad(0, -1, "Which planet is known as the Red Planet?", "Mars", "Mars has a reddish appearance."),
        Vocad(0, -1, "Who painted the Mona Lisa?", "Leonardo da Vinci", "He was a famous polymath."),
        Vocad(0, -1, "What is the largest ocean on Earth?", "Pacific Ocean", "It covers 30% of Earth."),
        Vocad(0, -1, "How many continents are there?", "7", "Africa, Asia, Europe, etc.")
    )

    private fun getModel(): GenerativeModel {
        // Use user key if available, otherwise fallback to SYSTEM_KEY for direct connect
        val keyToUse = if (apiKey.isNullOrBlank() || apiKey == "SYSTEM_DIRECT_CONNECT" || apiKey == "YOUR_API_KEY_HERE" || apiKey == "Verified_Account" || apiKey == "Mobile_Verified") {
            SYSTEM_KEY 
        } else {
            apiKey
        }
        return GenerativeModel(modelName = "gemini-1.5-flash", apiKey = keyToUse)
    }

    fun updateApiKey(newKey: String) {
        if (newKey.isNotBlank()) { apiKey = newKey }
    }

    suspend fun validateApiKey(testKey: String): Boolean = withContext(Dispatchers.IO) {
        if (testKey.isBlank()) return@withContext false
        try {
            val testModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = testKey)
            // A simple prompt to verify the key works
            val response = testModel.generateContent("Say 'ok'")
            response.text?.contains("ok", ignoreCase = true) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun cleanJson(text: String): String {
        var cleaned = text.trim()
        if (cleaned.contains("[")) {
            cleaned = cleaned.substring(cleaned.indexOf("["), cleaned.lastIndexOf("]") + 1)
        }
        return cleaned
    }

    suspend fun generateVocabs(topic: String, projectId: Int, language: String, level: String): List<Vocad> = withContext(Dispatchers.IO) {
        try {
            val response = getModel().generateContent(content { 
                text("Act as a Google Learning Expert. Generate 10 vocabulary words about '$topic' for '$level' difficulty. Primary language: '$language'. Return raw JSON array: [{\"word\":\"...\", \"definition\":\"...\", \"example\":\"...\"}]")
            })
            val jsonString = response.text ?: "[]"
            val jsonArray = JSONArray(cleanJson(jsonString))
            val result = mutableListOf<Vocad>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(Vocad(id = 0, projectId = projectId, word = obj.getString("word"), definition = obj.getString("definition"), example = obj.optString("example")))
            }
            result
        } catch (e: Exception) {
            // Fallback sample
            listOf(Vocad(0, projectId, "Welcome", "مرحباً", "Sample Word"))
        }
    }

    suspend fun generateQuestions(topic: String, count: Int): List<Vocad> = withContext(Dispatchers.IO) {
        val topicPrompt = if (topic.isBlank()) "random interesting trivia facts from Google's database" else "trivia questions about '$topic'"
        try {
            val response = getModel().generateContent(content { 
                text("Act as a Google Knowledge expert. Generate $count $topicPrompt. Return raw JSON array: [{\"word\":\"Question?\", \"definition\":\"Correct Answer\", \"example\":\"Additional interesting context/fact\"}]")
            })
            val jsonString = response.text ?: "[]"
            val jsonArray = JSONArray(cleanJson(jsonString))
            val result = mutableListOf<Vocad>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(Vocad(id = 0, projectId = -1, word = obj.getString("word"), definition = obj.getString("definition"), example = obj.optString("example")))
            }
            if (result.isEmpty()) localTrivia.shuffled().take(count) else result
        } catch (e: Exception) {
            localTrivia.shuffled().take(count)
        }
    }
}
