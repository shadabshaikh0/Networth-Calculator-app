package com.shadabshaikh.networth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shadabshaikh.networth.model.SnapshotData
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// One DataStore instance per process, tied to the app Context.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "networth")

/**
 * The single owner of on-device persistence. Stores the whole [SnapshotData] as
 * one JSON string plus the theme — the Android analog of the web's localStorage.
 * Everything is suspend (async, off the main thread).
 */
class LocalStore(context: Context) {

    private val store = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    /** Load persisted data, or the seeded sample portfolio on first run.
     *  Does NOT write the seed — [isTouched] stays false until a real edit. */
    suspend fun load(): SnapshotData {
        val raw = store.data.first()[SNAPSHOT]
        return raw?.let { runCatching { json.decodeFromString<SnapshotData>(it) }.getOrNull() }
            ?: seeded()
    }

    suspend fun save(data: SnapshotData) {
        store.edit { it[SNAPSHOT] = json.encodeToString(data) }
    }

    suspend fun loadTheme(): String = store.data.first()[THEME] ?: "dark"

    suspend fun saveTheme(theme: String) {
        store.edit { it[THEME] = theme }
    }

    /** True once the user has made any real edit (used by Module 4's sync
     *  reconcile to tell "untouched demo seed" from real data). */
    suspend fun isTouched(): Boolean = store.data.first()[TOUCHED] == true

    suspend fun markTouched() {
        store.edit { it[TOUCHED] = true }
    }

    /** Cached spreadsheet id, so we can skip findSheet on later launches. */
    suspend fun loadSheetId(): String? = store.data.first()[SHEET_ID]

    suspend fun saveSheetId(id: String?) {
        store.edit { if (id == null) it.remove(SHEET_ID) else it[SHEET_ID] = id }
    }

    private fun seeded(): SnapshotData = SnapshotData(
        assets = SEED_ASSETS,
        liab = SEED_LIAB,
        members = DEFAULT_MEMBERS,
        included = DEFAULT_MEMBERS.associate { it.id to true },
    )

    private companion object {
        val SNAPSHOT = stringPreferencesKey("snapshot")
        val THEME = stringPreferencesKey("theme")
        val TOUCHED = booleanPreferencesKey("touched")
        val SHEET_ID = stringPreferencesKey("sheetId")
    }
}
