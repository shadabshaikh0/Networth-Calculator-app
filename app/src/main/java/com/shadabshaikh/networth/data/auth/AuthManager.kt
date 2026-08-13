package com.shadabshaikh.networth.data.auth

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/** The signed-in account shown in the chip. */
data class Account(val email: String?, val name: String?)

/** Thrown when a token can't be obtained silently and interactive sign-in is needed. */
class NeedsSignIn : Exception("Interactive sign-in required")

/**
 * Obtains an OAuth access token scoped to `drive.file` via the Google
 * Authorization API — the native equivalent of the web app's `googleAuth.ts`.
 * Google identifies the app by package + SHA-1 (the Android OAuth client), so
 * no client-ID string is needed here.
 */
class AuthManager(context: Context) {

    private val client = Identity.getAuthorizationClient(context.applicationContext)
    private val signInClient = Identity.getSignInClient(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var accessToken: String? = null
    @Volatile private var tokenExpiry: Long = 0L

    private val request: AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(
            listOf(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("email"),
                Scope("profile"),
            ),
        )
        .build()

    /** A cached, non-expired token, or null. */
    fun currentToken(): String? = accessToken?.takeIf { System.currentTimeMillis() < tokenExpiry }

    /** Suspend accessor for the REST layer: cached token, or a silent
     *  re-authorization (works when a grant already exists). Throws
     *  [NeedsSignIn] if interactive consent would be required. */
    suspend fun getValidToken(): String {
        currentToken()?.let { return it }
        return suspendCancellableCoroutine { cont ->
            authorize(
                onSuccess = { token, _ -> if (cont.isActive) cont.resume(token) },
                onNeedConsent = { if (cont.isActive) cont.resumeWithException(NeedsSignIn()) },
                onError = { if (cont.isActive) cont.resumeWithException(it) },
            )
        }
    }

    /**
     * Ask for authorization. If the grant already exists, [onToken] fires with a
     * token; otherwise [onNeedConsent] gets a [PendingIntent] the UI must launch
     * to show Google's account-picker / consent screen.
     */
    fun authorize(
        onSuccess: (token: String, account: Account?) -> Unit,
        onNeedConsent: (PendingIntent) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        client.authorize(request)
            .addOnSuccessListener { result ->
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    onNeedConsent(pending)
                } else {
                    emit(result, onSuccess, onError)
                }
            }
            .addOnFailureListener(onError)
    }

    /** Complete an interactive consent from the launcher's result intent. */
    fun handleConsentResult(
        intent: Intent?,
        onSuccess: (token: String, account: Account?) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        runCatching { client.getAuthorizationResultFromIntent(intent) }
            .onSuccess { emit(it, onSuccess, onError) }
            .onFailure(onError)
    }

    private fun emit(
        result: AuthorizationResult,
        onSuccess: (token: String, account: Account?) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val token = result.accessToken
        if (token != null) {
            accessToken = token
            tokenExpiry = System.currentTimeMillis() + 55 * 60 * 1000L // tokens last ~1h; refresh early
            // Read identity straight from the grant (no network) when available.
            val account = runCatching {
                result.toGoogleSignInAccount()?.let { gsa ->
                    Account(email = gsa.email, name = gsa.displayName ?: gsa.givenName)
                }
            }.getOrNull()
            onSuccess(token, account)
        } else {
            onError(IllegalStateException("Authorization granted no access token"))
        }
    }

    /** Fetch the account's email/name. Tries Drive's `about` endpoint first —
     *  it works with the `drive.file` scope we already hold — then falls back to
     *  the OpenID userinfo endpoint. */
    suspend fun fetchUserInfo(token: String): Account? = withContext(Dispatchers.IO) {
        // Drive about.get supports the drive.file scope and returns the user.
        getJson("https://www.googleapis.com/drive/v3/about?fields=user", token)
            ?.get("user")?.jsonObject?.let { user ->
                val email = user["emailAddress"]?.jsonPrimitive?.contentOrNull
                val name = user["displayName"]?.jsonPrimitive?.contentOrNull
                if (email != null || name != null) return@withContext Account(email, name)
            }
        // Fallback: OpenID userinfo (needs email/profile scopes on the token).
        getJson("https://www.googleapis.com/oauth2/v3/userinfo", token)?.let { obj ->
            return@withContext Account(
                email = obj["email"]?.jsonPrimitive?.contentOrNull,
                name = obj["name"]?.jsonPrimitive?.contentOrNull,
            )
        }
        null
    }

    private fun getJson(urlStr: String, token: String): kotlinx.serialization.json.JsonObject? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 15000
            readTimeout = 15000
        }
        if (conn.responseCode != 200) return@runCatching null
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        json.parseToJsonElement(body).jsonObject
    }.getOrNull()

    /** Drop the cached token so the next [getValidToken] fetches a fresh one. */
    fun invalidateToken() {
        accessToken = null
        tokenExpiry = 0L
    }

    fun signOut() {
        accessToken = null
        tokenExpiry = 0L
        // Clear the cached Google credential so the next sign-in re-prompts cleanly.
        runCatching { signInClient.signOut() }
    }
}
