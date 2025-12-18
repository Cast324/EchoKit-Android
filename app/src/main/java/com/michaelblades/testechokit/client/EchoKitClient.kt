package com.michaelblades.testechokit.client

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * EchoKit Android Client
 *
 * A Kotlin client for integrating EchoKit feedback system into Android apps.
 *
 * Usage:
 * ```
 * val client = EchoKitClient(
 *     context = applicationContext,
 *     baseURL = "https://your-echokit-server.com",
 *     apiKey = "ek_your_api_key_here",
 *     userEmail = "user@example.com",
 *     userName = "John Doe"
 * )
 *
 * // Create an idea
 * val idea = client.createIdea("Add dark mode", "Would love dark mode support")
 *
 * // Get all ideas
 * val ideas = client.getIdeas(onlyApproved = true)
 *
 * // Vote on an idea
 * client.vote(ideaId)
 * ```
 */
@OptIn(InternalSerializationApi::class)
class EchoKitClient(
    context: Context,
    private val baseURL: String,
    private val apiKey: String,
    private val userEmail: String? = null,
    private val userName: String? = null
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "EchoKitPrefs",
        Context.MODE_PRIVATE
    )

    private val userId: String = getOrCreateUserId()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun getOrCreateUserId(): String {
        val key = "EchoKitUserId"
        return prefs.getString(key, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit { putString(key, newId) }
            newId
        }
    }

    // MARK: - Models

    @Serializable
    enum class IdeaStatus {
        @SerialName("pending")
        PENDING,
        @SerialName("in-progress")
        IN_PROGRESS,
        @SerialName("completed")
        COMPLETED;

        val displayName: String
            get() = when (this) {
                PENDING -> "Pending"
                IN_PROGRESS -> "In Progress"
                COMPLETED -> "Completed"
            }
    }

    @Serializable
    enum class IdeaCategory {
        @SerialName("new-idea")
        NEW_IDEA,
        @SerialName("feature")
        FEATURE,
        @SerialName("enhancement")
        ENHANCEMENT,
        @SerialName("integration")
        INTEGRATION,
        @SerialName("ui-ux")
        UI_UX;

        val displayName: String
            get() = when (this) {
                NEW_IDEA -> "New Idea"
                FEATURE -> "Feature"
                ENHANCEMENT -> "Enhancement"
                INTEGRATION -> "Integration"
                UI_UX -> "UI/UX"
            }

        val icon: String
            get() = when (this) {
                NEW_IDEA -> "💡"
                FEATURE -> "⭐"
                ENHANCEMENT -> "⬆️"
                INTEGRATION -> "🧩"
                UI_UX -> "🎨"
            }
    }

    @Serializable
    data class Idea(
        val id: String,
        val title: String,
        val body: String? = null,
        val status: IdeaStatus,
        val category: IdeaCategory,
        val isApproved: Boolean,
        val voteCount: Int,
        val commentCount: Int,
        val createdBy: String,
        val createdAt: String? = null,
        val userHasVoted: Boolean
    )

    @Serializable
    data class IdeaDetail(
        val id: String,
        val title: String,
        val body: String? = null,
        val status: IdeaStatus,
        val category: IdeaCategory,
        val isApproved: Boolean,
        val voteCount: Int,
        val commentCount: Int,
        val createdBy: String,
        val createdAt: String? = null,
        val comments: List<Comment>,
        val userHasVoted: Boolean
    )

    @Serializable
    data class Comment(
        val id: String,
        val body: String,
        val createdBy: String,
        val createdAt: String? = null
    )

    @Serializable
    private data class CreateIdeaRequest(
        val title: String,
        val body: String?,
        val userId: String,
        val userEmail: String?,
        val userName: String?
    )

    @Serializable
    private data class CreateVoteRequest(
        val ideaId: String,
        val userId: String,
        val userEmail: String?,
        val userName: String?,
        val voteType: String
    )

    @Serializable
    private data class CreateCommentRequest(
        val ideaId: String,
        val body: String,
        val userId: String,
        val userEmail: String?,
        val userName: String?
    )

    // MARK: - API Methods

    suspend fun createIdea(title: String, body: String? = null): Idea {
        val request = CreateIdeaRequest(
            title = title,
            body = body,
            userId = userId,
            userEmail = userEmail,
            userName = userName
        )

        return post("/api/ideas", request)
    }

    suspend fun getIdeas(
        status: IdeaStatus? = null,
        onlyApproved: Boolean = false
    ): List<Idea> {
        val url = buildString {
            append("$baseURL/api/ideas?userId=$userId")
            if (status != null) {
                append("&status=${status.name.lowercase().replace('_', '-')}")
            }
            if (onlyApproved) {
                append("&approved=true")
            }
        }

        return get(url)
    }

    suspend fun getIdeaDetail(ideaId: String): IdeaDetail {
        val url = "$baseURL/api/ideas/$ideaId?userId=$userId"
        return get(url)
    }

    suspend fun vote(ideaId: String) {
        val request = CreateVoteRequest(
            ideaId = ideaId,
            userId = userId,
            userEmail = userEmail,
            userName = userName,
            voteType = "up"
        )

        postWithoutResponse("/api/votes", request)
    }

    suspend fun addComment(ideaId: String, body: String): Comment {
        val request = CreateCommentRequest(
            ideaId = ideaId,
            body = body,
            userId = userId,
            userEmail = userEmail,
            userName = userName
        )

        return post("/api/comments", request)
    }

    // MARK: - Helper Methods

    private suspend inline fun <reified T> get(url: String): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Request failed: ${response.code}")
            }

            val body = response.body?.string() ?: throw IOException("Empty response")
            json.decodeFromString(body)
        }
    }

    private suspend inline fun <reified T, reified R> post(endpoint: String, body: T): R =
        withContext(Dispatchers.IO) {
            val jsonBody = json.encodeToString(body)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseURL$endpoint")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Request failed: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                json.decodeFromString(responseBody)
            }
        }

    private suspend inline fun <reified T> postWithoutResponse(endpoint: String, body: T) =
        withContext(Dispatchers.IO) {
            val jsonBody = json.encodeToString(body)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseURL$endpoint")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Request failed: ${response.code}")
                }
            }
        }
}
