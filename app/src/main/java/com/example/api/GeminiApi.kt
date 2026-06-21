package com.example.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: okhttp3.RequestBody
    ): ResponseBody
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("RetrofitClient", "Executing: ${request.url}")
            chain.proceed(request)
        }
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

data class ParsedTask(val name: String, val note: String?)

object OrbitGeminiManager {
    suspend fun organizeBrainDump(dump: String): List<ParsedTask> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("OrbitGeminiManager", "Gemini API Key is missing or default placeholder!")
            return fallbackOrganize(dump)
        }

        val prompt = """
            You are Orbit, a gentle space companion. Convert the following paragraph of thoughts (brain dump) into a list of specific, separate, clean tasks. Make sure the tasks are comforting, friendly, and simple.
            
            Strict Splitting Rules:
            - Split tasks separated by commas (,)
            - Split tasks separated by semicolons (;)
            - Split tasks separated by line breaks
            - Split tasks separated by "and" (e.g. "Buy books and do homework" MUST be split into "Buy books" and "Do homework")
            
            Raw thoughts: $dump
            
            Format the response strictly as a JSON array of objects (one object per task). Do not wrap it in any formatting other than clean JSON. Do not include markdown formatting markers like ```json ... ```.
            Example format:
            [
              {"name": "Finish chemistry assignment", "note": "Read chapter 4 first"},
              {"name": "Call grandma", "note": null}
            ]
        """.trimIndent()

        // Construct request JSON manually to avoid any serialization library conflicts
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("responseFormat", JSONObject().put("text", JSONObject().put("mimeType", "application/json")))
                put("temperature", 0.2)
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(
                    JSONObject().put("text", "You are Orbit, a cozy space companion. You help users declutter their busy minds with kindness and zero pressure.")
                ))
            })
        }

        val mediaType = "application/json".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        return try {
            val responseBody = RetrofitClient.service.generateContent(apiKey, requestBody)
            val jsonText = responseBody.string()
            Log.d("OrbitGeminiManager", "Gemini Response: $jsonText")
            
            val jsonResponseObj = JSONObject(jsonText)
            val candidates = jsonResponseObj.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val partText = parts?.optJSONObject(0)?.optString("text")

            if (!partText.isNullOrEmpty()) {
                parseTasksFromJson(partText)
            } else {
                fallbackOrganize(dump)
            }
        } catch (e: Exception) {
            Log.e("OrbitGeminiManager", "Error calling Gemini API: ${e.message}", e)
            fallbackOrganize(dump)
        }
    }

    private fun parseTasksFromJson(jsonStr: String): List<ParsedTask> {
        val list = mutableListOf<ParsedTask>()
        try {
            // Un-escape markdown block if it was somehow appended
            var cleanedStr = jsonStr.trim()
            if (cleanedStr.contains("```")) {
                cleanedStr = cleanedStr.replace("```json", "").replace("```", "").trim()
            }
            
            val array = JSONArray(cleanedStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name") ?: ""
                if (name.isNotEmpty()) {
                    val noteVal = obj.optString("note", "")
                    val note = if (noteVal.isEmpty() || noteVal == "null") null else noteVal
                    list.add(ParsedTask(name = name, note = note))
                }
            }
        } catch (e: Exception) {
            Log.e("OrbitGeminiManager", "JSON parsing failed for string: $jsonStr", e)
            return fallbackOrganize(jsonStr)
        }
        return list
    }

    // A robust, offline fallback if the API fails or key is missing
    fun fallbackOrganize(dump: String): List<ParsedTask> {
        if (dump.isBlank()) return emptyList()
        // Split by newlines, semicolons, commas, and case-insensitive "and" with word boundaries
        val regex = Regex("\\s*(?:\\r?\\n|;|,|\\b(?i)and\\b)\\s*")
        return dump.split(regex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { taskText ->
                var cleaned = taskText
                    .removePrefix("-")
                    .removePrefix("*")
                    .removePrefix("•")
                    .trim()
                cleaned = cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                ParsedTask(name = cleaned, note = null)
            }
            .filter { it.name.length >= 2 }
    }
}
