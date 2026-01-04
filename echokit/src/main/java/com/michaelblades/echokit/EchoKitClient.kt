package com.michaelblades.echokit

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*

/**
 * EchoKit Android Client
 *
 * A feature request and feedback management client for Android
 */
@SuppressLint("UnsafeOptInUsageError")
class EchoKitClient(
    private val apiKey: String,
    private val userEmail: String? = null,
    private val userName: String? = null,
    context: Context
) {
    private val baseURL = "https://www.echokit.app"
    private val prefs: SharedPreferences = context.getSharedPreferences("EchoKit", Context.MODE_PRIVATE)
    private val userId: String = getOrCreateUserId()
    private val httpClient = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun getOrCreateUserId(): String {
        val key = "EchoKitUserId"
        val existingId = prefs.getString(key, null)

        return if (existingId != null) {
            existingId
        } else {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(key, newId).apply()
            newId
        }
    }

    // MARK: - Enums

    @Serializable
    enum class IdeaStatus(val value: String) {
        @SerialName("pending")
        PENDING("pending"),

        @SerialName("in-progress")
        IN_PROGRESS("in-progress"),

        @SerialName("completed")
        COMPLETED("completed");

        val displayName: String
            get() = when (this) {
                PENDING -> "Pending"
                IN_PROGRESS -> "In Progress"
                COMPLETED -> "Completed"
            }
    }

    @Serializable
    enum class IdeaCategory(val value: String) {
        @SerialName("new-idea")
        NEW_IDEA("new-idea"),

        @SerialName("feature")
        FEATURE("feature"),

        @SerialName("enhancement")
        ENHANCEMENT("enhancement"),

        @SerialName("integration")
        INTEGRATION("integration"),

        @SerialName("ui-ux")
        UI_UX("ui-ux");

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
                NEW_IDEA -> "lightbulb"
                FEATURE -> "star"
                ENHANCEMENT -> "arrow_upward"
                INTEGRATION -> "extension"
                UI_UX -> "palette"
            }
    }

    // MARK: - Models

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

    suspend fun createIdea(title: String, body: String?): Idea = withContext(Dispatchers.IO) {
        val request = CreateIdeaRequest(
            title = title,
            body = body,
            userId = userId,
            userEmail = userEmail,
            userName = userName
        )
        post("/api/ideas", request)
    }

    suspend fun getIdeas(
        status: IdeaStatus? = null,
        onlyApproved: Boolean = false
    ): List<Idea> = withContext(Dispatchers.IO) {
        val urlBuilder = "$baseURL/api/ideas".toHttpUrlOrNull()!!.newBuilder()

        status?.let { urlBuilder.addQueryParameter("status", it.value) }
        if (onlyApproved) urlBuilder.addQueryParameter("approved", "true")
        urlBuilder.addQueryParameter("userId", userId)

        get(urlBuilder.build())
    }

    suspend fun getIdeaDetail(ideaId: String): IdeaDetail = withContext(Dispatchers.IO) {
        val url = "$baseURL/api/ideas/$ideaId".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("userId", userId)
            .build()
        get(url)
    }

    suspend fun vote(ideaId: String): Unit = withContext(Dispatchers.IO) {
        val request = CreateVoteRequest(
            ideaId = ideaId,
            userId = userId,
            userEmail = userEmail,
            userName = userName,
            voteType = "up"
        )
        postWithoutResponse("/api/votes", request)
    }

    suspend fun addComment(ideaId: String, body: String): Comment = withContext(Dispatchers.IO) {
        val request = CreateCommentRequest(
            ideaId = ideaId,
            body = body,
            userId = userId,
            userEmail = userEmail,
            userName = userName
        )
        post("/api/comments", request)
    }

    // MARK: - Helper Methods

    private inline fun <reified T> get(url: HttpUrl): T {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .cacheControl(CacheControl.Builder().noCache().build())
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Request failed: ${'$'}{response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response")
            return json.decodeFromString(body)
        }
    }

    private inline fun <reified T, reified R> post(endpoint: String, body: T): R {
        val jsonBody = json.encodeToString(body)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseURL$endpoint")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Request failed: ${'$'}{response.code}")
            val responseBody = response.body?.string() ?: throw IOException("Empty response")
            return json.decodeFromString(responseBody)
        }
    }

    private inline fun <reified T> postWithoutResponse(endpoint: String, body: T) {
        val jsonBody = json.encodeToString(body)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseURL$endpoint")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Request failed: ${'$'}{response.code}")
        }
    }

    sealed class EchoKitError : Exception() {
        object RequestFailed : EchoKitError()
        object Cancelled : EchoKitError()
    }
}