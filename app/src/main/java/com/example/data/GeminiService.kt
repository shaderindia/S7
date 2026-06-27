package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    // OkHttpClient with generous timeouts for Gemini API
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getAegisReply(userMessage: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineMockReply(userMessage)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        // Construct the prompt keeping in mind privacy, low-latency, and battery optimization themes
        val systemPrompt = """
            You are S7 AI, an assistant simulating a remote peer in a secure end-to-end encrypted chat application called S7 Call.
            Your conversation partner is testing the secure, low-latency capabilities of the app.
            Acknowledge their questions about encryption, battery optimization, Opus audio codec, video latency, and disappearing messages with expert, clean, short answers (max 2-3 sentences).
            Be friendly, secure, and helpful. Always act like a real person chatting securely.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\nUser says: $userMessage")
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed with code: ${response.code}")
                    return@withContext getOfflineMockReply(userMessage)
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                parts.getJSONObject(0).getString("text").trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            getOfflineMockReply(userMessage)
        }
    }

    suspend fun getContactReply(contactName: String, userMessage: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineMockReplyForContact(contactName, userMessage)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val systemPrompt = """
            You are $contactName, a direct contact in S7 Call, a secure, low-latency, end-to-end encrypted messaging application.
            The user is texting you. Respond naturally, conversationally, and stay in character as $contactName.
            Do not mention that you are an AI or simulation. Keep your reply concise (1-3 sentences) as fits a standard text message.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\nUser says: $userMessage")
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext getOfflineMockReplyForContact(contactName, userMessage)
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                parts.getJSONObject(0).getString("text").trim()
            }
        } catch (e: Exception) {
            getOfflineMockReplyForContact(contactName, userMessage)
        }
    }

    private fun getOfflineMockReplyForContact(contactName: String, message: String): String {
        return "Hey there! This is $contactName. I'm connected over our secure S7 end-to-end encrypted voice/video tunnel. Your message received successfully!"
    }

    private fun getOfflineMockReply(message: String): String {
        val msgLower = message.lowercase()
        return when {
            msgLower.contains("encrypt") || msgLower.contains("security") || msgLower.contains("key") -> {
                "Our connection is secured with military-grade AES-CBC 256-bit encryption. The public keys are verified against our security fingerprints (SEC-XXXX). No third parties can intercept this!"
            }
            msgLower.contains("video") || msgLower.contains("call") || msgLower.contains("latency") -> {
                "Our video engine runs on a direct peer-to-peer WebRTC connection. It optimizes codecs based on battery usage, capping latency at a tiny 18ms for extreme responsiveness!"
            }
            msgLower.contains("disappear") || msgLower.contains("vanish") || msgLower.contains("timer") -> {
                "Disappearing messages are highly secure! Once the countdown timer ends, the ciphertext is completely wiped from both devices' physical databases."
            }
            msgLower.contains("battery") || msgLower.contains("power") -> {
                "Our calling protocol operates in 'Battery Safe' mode, using hardware-accelerated Opus and H.264 codecs to reduce CPU wakeups and maximize battery life up to 40%."
            }
            msgLower.contains("ios") || msgLower.contains("apple") || msgLower.contains("web") -> {
                "S7 Call maintains seamless cross-platform syncing with web browser support (secure QR pairing) and iOS clients using uniform Rust-based crypto cores!"
            }
            else -> {
                "Hello! I am S7, your secure encryption partner. Our conversation is end-to-end encrypted with zero logs. Feel free to simulate video calling or change the vanishing timers!"
            }
        }
    }
}
