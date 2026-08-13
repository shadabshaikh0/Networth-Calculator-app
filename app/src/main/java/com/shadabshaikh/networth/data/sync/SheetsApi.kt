package com.shadabshaikh.networth.data.sync

import com.shadabshaikh.networth.data.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.net.HttpURLConnection
import java.net.URL

/** A non-2xx response from a Google REST endpoint. */
class HttpError(val code: Int, val bodySnippet: String) : Exception("HTTP $code: $bodySnippet")

/**
 * Low-level authenticated JSON calls to the Drive/Sheets REST APIs. Adds the
 * Bearer token, and on a 401 refreshes the token once and retries — mirroring
 * the web app's `api()` in `googleSheets.ts`.
 */
class SheetsApi(private val auth: AuthManager) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun request(method: String, url: String, body: JsonElement? = null): JsonElement =
        withContext(Dispatchers.IO) {
            try {
                call(method, url, body, auth.getValidToken())
            } catch (e: HttpError) {
                if (e.code == 401) {
                    auth.invalidateToken()
                    call(method, url, body, auth.getValidToken())
                } else {
                    throw e
                }
            }
        }

    private fun call(method: String, url: String, body: JsonElement?, token: String): JsonElement {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 20_000
            readTimeout = 20_000
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw HttpError(code, err.take(300))
        }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        return json.parseToJsonElement(text.ifBlank { "{}" })
    }
}
