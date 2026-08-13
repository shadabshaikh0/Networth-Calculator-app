package com.shadabshaikh.networth.data.sync

import com.shadabshaikh.networth.model.Item
import com.shadabshaikh.networth.model.Member
import com.shadabshaikh.networth.model.Metal
import com.shadabshaikh.networth.model.Snapshot
import com.shadabshaikh.networth.model.SnapshotData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.URLEncoder

/**
 * The Google Sheet contract, ported byte-for-byte from the web app's
 * `lib/googleSheets.ts` so both platforms read/write the same spreadsheet.
 */
class SheetsRepository(private val api: SheetsApi) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Find the app's spreadsheet (created by us, tagged via appProperties). */
    suspend fun findSheet(): String? {
        val q = "mimeType='application/vnd.google-apps.spreadsheet' and trashed=false and " +
            "appProperties has { key='$APP_PROP_KEY' and value='$APP_PROP_VALUE' }"
        val url = "$DRIVE?q=${enc(q)}&spaces=drive&fields=${enc("files(id,modifiedTime)")}" +
            "&orderBy=${enc("modifiedTime desc")}"
        val files = api.request("GET", url).jsonObject["files"]?.jsonArray ?: return null
        return files.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
    }

    /** Create the spreadsheet with our four tabs, then stamp the Drive marker. */
    suspend fun createSheet(): String {
        val body = buildJsonObject {
            putJsonObject("properties") { put("title", SHEET_TITLE) }
            putJsonArray("sheets") {
                TABS.forEach { t -> addJsonObject { putJsonObject("properties") { put("title", t) } } }
            }
        }
        val id = api.request("POST", SHEETS, body).jsonObject["spreadsheetId"]!!.jsonPrimitive.content
        val patch = buildJsonObject { putJsonObject("appProperties") { put(APP_PROP_KEY, APP_PROP_VALUE) } }
        api.request("PATCH", "$DRIVE/$id", patch)
        return id
    }

    /** Read all tabs and assemble a snapshot. */
    suspend fun loadAll(id: String): SnapshotData {
        val ranges = TABS.joinToString("&") { "ranges=${enc("$it!A1:Z1000")}" }
        val res = api.request("GET", "$SHEETS/$id/values:batchGet?$ranges&valueRenderOption=UNFORMATTED_VALUE").jsonObject
        val valueRanges = res["valueRanges"]?.jsonArray ?: JsonArray(emptyList())

        fun rows(idx: Int): List<List<String>> {
            val values = valueRanges.getOrNull(idx)?.jsonObject?.get("values")?.jsonArray ?: return emptyList()
            return values.drop(1).map { row -> row.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } }
        }

        val assets = rows(0).mapNotNull(::rowToItem)
        val liab = rows(1).mapNotNull(::rowToItem)
        val members = rows(2).mapNotNull { r ->
            val mid = r.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Member(mid, r.getOrElse(1) { "" }, r.getOrElse(2) { "" }, r.getOrElse(3) { "#D5B475" }.ifEmpty { "#D5B475" })
        }
        val meta = HashMap<String, String>()
        rows(3).forEach { r -> r.getOrNull(0)?.takeIf { it.isNotEmpty() }?.let { meta[it] = r.getOrElse(1) { "" } } }

        return SnapshotData(
            assets = assets,
            liab = liab,
            members = members,
            included = parseBoolMap(meta["included"]),
            rates = parseLongMap(meta["rates"]),
            onboardDismissed = meta["onboardDismissed"] == "1",
            history = parseHistory(meta["history"]),
        )
    }

    /** Overwrite all tabs with the snapshot (clear then write). */
    suspend fun saveAll(id: String, data: SnapshotData) {
        val clear = buildJsonObject { putJsonArray("ranges") { TABS.forEach { add("$it!A1:Z1000") } } }
        api.request("POST", "$SHEETS/$id/values:batchClear", clear)

        val metaRows = listOf(
            row("schemaVersion", SCHEMA_VERSION),
            row("currency", "INR"),
            row("included", json.encodeToString(data.included)),
            row("rates", json.encodeToString(data.rates)),
            row("history", json.encodeToString(data.history)),
            row("onboardDismissed", if (data.onboardDismissed) "1" else "0"),
        )
        val update = buildJsonObject {
            put("valueInputOption", "RAW")
            putJsonArray("data") {
                addJsonObject { put("range", "Assets!A1"); put("values", matrix(ITEM_COLS, data.assets.map(::itemToRow))) }
                addJsonObject { put("range", "Liabilities!A1"); put("values", matrix(ITEM_COLS, data.liab.map(::itemToRow))) }
                addJsonObject { put("range", "Members!A1"); put("values", matrix(MEMBER_COLS, data.members.map { listOf(str(it.id), str(it.name), str(it.relation), str(it.color)) })) }
                addJsonObject { put("range", "Meta!A1"); put("values", matrix(listOf("key", "value"), metaRows)) }
            }
        }
        api.request("POST", "$SHEETS/$id/values:batchUpdate", update)
    }

    // ---- serialization ----
    private fun itemToRow(i: Item): List<JsonElement> = listOf(
        str(i.id), str(i.name), str(i.cat), JsonPrimitive(i.value), str(i.owner),
        str(if (i.hidden) "TRUE" else "FALSE"), str(i.note ?: ""), str(i.ref ?: ""),
        i.grams?.let { JsonPrimitive(it) } ?: str(""), str(i.metal?.key ?: ""),
    )

    private fun rowToItem(r: List<String>): Item? {
        val id = r.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return null
        val hidden = r.getOrElse(5) { "" }.let { it == "TRUE" || it == "true" || it == "1" }
        return Item(
            id = id,
            name = r.getOrElse(1) { "" },
            cat = r.getOrElse(2) { "" },
            value = r.getOrElse(3) { "" }.toDoubleOrNull()?.toLong() ?: 0L,
            owner = r.getOrElse(4) { "" }.ifEmpty { "self" },
            hidden = hidden,
            note = r.getOrElse(6) { "" }.ifEmpty { null },
            ref = r.getOrElse(7) { "" }.ifEmpty { null },
            grams = r.getOrElse(8) { "" }.toDoubleOrNull(),
            metal = when (r.getOrElse(9) { "" }) { "gold" -> Metal.GOLD; "silver" -> Metal.SILVER; else -> null },
        )
    }

    private fun parseBoolMap(s: String?): Map<String, Boolean> =
        if (s.isNullOrBlank()) emptyMap() else runCatching {
            Json.parseToJsonElement(s).jsonObject.mapValues { it.value.jsonPrimitive.content == "true" }
        }.getOrDefault(emptyMap())

    private fun parseLongMap(s: String?): Map<String, Long> =
        if (s.isNullOrBlank()) emptyMap() else runCatching {
            Json.parseToJsonElement(s).jsonObject.mapValues { it.value.jsonPrimitive.content.toDouble().toLong() }
        }.getOrDefault(emptyMap())

    private fun parseHistory(s: String?): List<Snapshot> =
        if (s.isNullOrBlank()) emptyList() else runCatching {
            Json.parseToJsonElement(s).jsonArray.mapNotNull { el ->
                val o = el.jsonObject
                val m = o["month"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                Snapshot(m, o["value"]?.jsonPrimitive?.content?.toDouble()?.toLong() ?: 0L)
            }
        }.getOrDefault(emptyList())

    private fun str(s: String): JsonElement = JsonPrimitive(s)
    private fun row(vararg cells: String): List<JsonElement> = cells.map(::str)
    private fun matrix(header: List<String>, rows: List<List<JsonElement>>): JsonArray = buildJsonArray {
        add(buildJsonArray { header.forEach { add(JsonPrimitive(it)) } })
        rows.forEach { r -> add(buildJsonArray { r.forEach { add(it) } }) }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private companion object {
        const val SHEETS = "https://sheets.googleapis.com/v4/spreadsheets"
        const val DRIVE = "https://www.googleapis.com/drive/v3/files"
        const val SHEET_TITLE = "Net worth data"
        const val SCHEMA_VERSION = "1"
        const val APP_PROP_KEY = "networthApp"
        const val APP_PROP_VALUE = "1"
        val TABS = listOf("Assets", "Liabilities", "Members", "Meta")
        val ITEM_COLS = listOf("id", "name", "cat", "value", "owner", "hidden", "note", "ref", "grams", "metal")
        val MEMBER_COLS = listOf("id", "name", "relation", "color")
    }
}
