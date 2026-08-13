package com.shadabshaikh.networth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shadabshaikh.networth.data.LocalStore
import com.shadabshaikh.networth.data.DEFAULT_MEMBERS
import com.shadabshaikh.networth.data.auth.Account
import com.shadabshaikh.networth.data.auth.AuthManager
import com.shadabshaikh.networth.data.sync.SheetsApi
import com.shadabshaikh.networth.data.sync.SheetsRepository
import com.shadabshaikh.networth.domain.buildCsv
import com.shadabshaikh.networth.domain.DeriveInput
import com.shadabshaikh.networth.domain.Derived
import com.shadabshaikh.networth.domain.derive
import com.shadabshaikh.networth.domain.netWorthOf
import com.shadabshaikh.networth.domain.recordSnapshot
import com.shadabshaikh.networth.data.SEED_ASSETS
import com.shadabshaikh.networth.data.SEED_LIAB
import com.shadabshaikh.networth.model.CatSel
import com.shadabshaikh.networth.model.Item
import com.shadabshaikh.networth.model.Kind
import com.shadabshaikh.networth.model.Member
import com.shadabshaikh.networth.model.Snapshot
import com.shadabshaikh.networth.model.SnapshotData
import com.shadabshaikh.networth.model.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

/** Raw, immutable app state. The single source of truth the UI reads. */
data class UiState(
    val assets: List<Item> = emptyList(),
    val liab: List<Item> = emptyList(),
    val members: List<Member> = emptyList(),
    val included: Map<String, Boolean> = emptyMap(),
    val rates: Map<String, Long> = emptyMap(),
    val history: List<Snapshot> = emptyList(),
    val onboardDismissed: Boolean = false,
    val theme: String = "dark", // "dark" | "light"
    val view: View = View.DASHBOARD,
    val catSel: CatSel? = null,
    val liqView: Boolean = false,
    val currentMonth: String = "",
    val editor: EditorTarget? = null,
    val showMembers: Boolean = false,
    val authStatus: AuthStatus = AuthStatus.SIGNED_OUT,
    val account: Account? = null,
    val showAccount: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val syncError: String? = null,
    val untouched: Boolean = false, // showing the auto-seeded sample (no real edits yet)
    val loaded: Boolean = false,
)

enum class AuthStatus { SIGNED_OUT, SIGNING_IN, SIGNED_IN }
enum class SyncStatus { IDLE, SYNCING, SYNCED, ERROR }

/** Which item editor sheet is open. [item] null = fresh add; an item with a
 *  blank id = add pre-filled into a category; an item with an id = edit. */
data class EditorTarget(val kind: Kind, val item: Item?)

private fun UiState.toSnapshotData() =
    SnapshotData(assets, liab, members, included, rates, onboardDismissed, history)

private fun UiState.toDeriveInput() =
    DeriveInput(assets, liab, members, included, rates, history, currentMonth, onboardDismissed, catSel)

class NetworthViewModel(app: Application) : AndroidViewModel(app) {

    private val store = LocalStore(app)

    val authManager = AuthManager(app)
    private val sheets = SheetsRepository(SheetsApi(authManager))

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Display-ready values, recomputed whenever raw state changes. */
    val derived: StateFlow<Derived> = _state
        .map { derive(it.toDeriveInput()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, derive(UiState().toDeriveInput()))

    init {
        viewModelScope.launch {
            val data = store.load()
            val theme = store.loadTheme()
            val touched = store.isTouched()
            val (members, included) = normalize(data)
            var s = UiState(
                assets = data.assets, liab = data.liab, members = members, included = included,
                rates = data.rates, history = data.history, onboardDismissed = data.onboardDismissed,
                theme = theme, currentMonth = currentMonth(), untouched = !touched, loaded = true,
            )
            s = s.recordIfNonEmpty()
            _state.value = s
            // Persist any history point recorded on load — does NOT mark touched.
            store.save(s.toSnapshotData())
        }
    }

    // ---- theme (persisted separately, never marks data as "touched") ----
    fun setTheme(theme: String) {
        _state.value = _state.value.copy(theme = theme)
        viewModelScope.launch { store.saveTheme(theme) }
    }
    fun toggleTheme() = setTheme(if (_state.value.theme == "light") "dark" else "light")

    // ---- ephemeral navigation / view state (not persisted) ----
    fun gotoDashboard() = setEphemeral { it.copy(view = View.DASHBOARD, catSel = null) }
    fun gotoHistory() = setEphemeral { it.copy(view = View.HISTORY, catSel = null) }
    fun openCategory(kind: Kind, key: String) =
        setEphemeral { it.copy(view = View.CATEGORY, catSel = CatSel(kind, key)) }
    fun toggleLiqView() = setEphemeral { it.copy(liqView = !it.liqView) }

    // ---- item editor open/close (ephemeral; the sheet owns the draft) ----
    fun openAdd(kind: Kind, cat: String? = null) = setEphemeral {
        it.copy(editor = EditorTarget(kind, cat?.let { c -> Item(id = "", name = "", cat = c, value = 0) }))
    }
    fun openEdit(kind: Kind, item: Item) = setEphemeral { it.copy(editor = EditorTarget(kind, item)) }
    fun openEditById(kind: Kind, id: String) {
        val list = if (kind == Kind.ASSET) _state.value.assets else _state.value.liab
        val item = list.firstOrNull { it.id == id } ?: return
        setEphemeral { it.copy(editor = EditorTarget(kind, item)) }
    }
    fun closeEditor() = setEphemeral { it.copy(editor = null) }

    fun openMembers() = setEphemeral { it.copy(showMembers = true) }
    fun closeMembers() = setEphemeral { it.copy(showMembers = false) }

    // ---- Google sign-in (Authorization API) ----

    /** Start sign-in. The consent PendingIntent (if any) is launched by the UI. */
    fun beginSignIn(onNeedConsent: (android.app.PendingIntent) -> Unit) {
        setEphemeral { it.copy(authStatus = AuthStatus.SIGNING_IN) }
        authManager.authorize(
            onSuccess = ::onSignedIn,
            onNeedConsent = onNeedConsent,
            onError = { setEphemeral { s -> s.copy(authStatus = AuthStatus.SIGNED_OUT) } },
        )
    }

    /** Called by the UI with the consent-screen result. */
    fun completeSignIn(intent: android.content.Intent?) {
        authManager.handleConsentResult(
            intent,
            onSuccess = ::onSignedIn,
            onError = { setEphemeral { s -> s.copy(authStatus = AuthStatus.SIGNED_OUT) } },
        )
    }

    /** The user backed out of / cancelled the Google consent screen. */
    fun cancelSignIn() = setEphemeral {
        if (it.authStatus == AuthStatus.SIGNING_IN) it.copy(authStatus = AuthStatus.SIGNED_OUT) else it
    }

    private fun onSignedIn(token: String, account: Account?) {
        // Flip to signed-in immediately, with whatever identity the grant gave us.
        setEphemeral { it.copy(authStatus = AuthStatus.SIGNED_IN, account = account) }
        // If the grant didn't carry email/name, fall back to the userinfo endpoint.
        if (account?.email == null) {
            viewModelScope.launch {
                val fetched = authManager.fetchUserInfo(token)
                if (fetched != null) setEphemeral { it.copy(account = fetched) }
            }
        }
        reconcile() // find/create the sheet and sync
    }

    fun signOut() {
        authManager.signOut()
        pushJob?.cancel()
        viewModelScope.launch { store.saveSheetId(null) }
        setEphemeral {
            it.copy(
                authStatus = AuthStatus.SIGNED_OUT, account = null, showAccount = false,
                syncStatus = SyncStatus.IDLE, syncError = null,
            )
        }
    }

    // ---- sheet sync (ports lib/sync.ts) ----

    private var pushJob: Job? = null
    private var pushing = false
    private var pushQueued = false

    private fun setSyncStatus(status: SyncStatus, error: String? = null) =
        setEphemeral { it.copy(syncStatus = status, syncError = error) }

    /** On sign-in: find or create the sheet and reconcile. Sheet wins if it
     *  exists; otherwise push local (unless it's the untouched demo seed). */
    private fun reconcile() {
        viewModelScope.launch {
            setSyncStatus(SyncStatus.SYNCING)
            try {
                var id = store.loadSheetId() ?: sheets.findSheet()
                if (id != null) {
                    hydrate(sheets.loadAll(id))
                } else {
                    id = sheets.createSheet()
                    if (!store.isTouched()) {
                        // Untouched demo seed — start the user's sheet clean.
                        hydrate(SnapshotData(members = listOf(DEFAULT_MEMBERS[0]), included = mapOf("self" to true)))
                    }
                    sheets.saveAll(id, _state.value.toSnapshotData())
                }
                store.saveSheetId(id)
                setSyncStatus(SyncStatus.SYNCED)
            } catch (e: Exception) {
                setSyncStatus(SyncStatus.ERROR, e.message)
            }
        }
    }

    /** Replace local state from a loaded sheet, and persist it locally. */
    private fun hydrate(data: SnapshotData) {
        val (members, included) = normalize(data)
        var s = _state.value.copy(
            assets = data.assets, liab = data.liab, members = members, included = included,
            rates = data.rates, history = data.history, onboardDismissed = data.onboardDismissed,
            untouched = false, loaded = true,
        )
        s = s.recordIfNonEmpty()
        _state.value = s
        viewModelScope.launch { store.markTouched(); store.save(s.toSnapshotData()) }
    }

    /** Debounced (~1s) push after any local change while signed in. */
    private fun schedulePush() {
        if (_state.value.authStatus != AuthStatus.SIGNED_IN) return
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            delay(1000)
            pushNow()
        }
    }

    private suspend fun pushNow() {
        val id = store.loadSheetId() ?: return
        if (pushing) { pushQueued = true; return }
        pushing = true
        setSyncStatus(SyncStatus.SYNCING)
        try {
            sheets.saveAll(id, _state.value.toSnapshotData())
            setSyncStatus(SyncStatus.SYNCED)
        } catch (e: Exception) {
            setSyncStatus(SyncStatus.ERROR, e.message)
        } finally {
            pushing = false
            if (pushQueued) { pushQueued = false; pushNow() }
        }
    }

    fun openAccount() = setEphemeral { it.copy(showAccount = true) }
    fun closeAccount() = setEphemeral { it.copy(showAccount = false) }

    // ---- data mutations (persisted, mark touched, re-record history) ----
    fun toggleMember(id: String) = update { s ->
        s.copy(included = s.included + (id to !(s.included[id] ?: true)))
    }

    fun setRate(metal: String, value: Long) = update { s ->
        s.copy(rates = s.rates + (metal to value))
    }

    fun dismissOnboard() = update { it.copy(onboardDismissed = true) }

    fun toggleHidden(kind: Kind, id: String) = update { s ->
        s.mapList(kind) { list -> list.map { if (it.id == id) it.copy(hidden = !it.hidden) else it } }
    }

    fun deleteItem(kind: Kind, id: String) = update { s ->
        s.mapList(kind) { list -> list.filterNot { it.id == id } }
    }

    /** Add (blank id) or replace (matching id) an item. */
    fun upsertItem(kind: Kind, item: Item) = update { s ->
        val withId = if (item.id.isBlank()) item.copy(id = newId("x")) else item
        s.mapList(kind) { list ->
            if (list.any { it.id == withId.id }) list.map { if (it.id == withId.id) withId else it }
            else list + withId
        }
    }

    fun loadSample() = update {
        it.copy(assets = SEED_ASSETS, liab = SEED_LIAB, view = View.DASHBOARD, catSel = null)
    }

    fun clearAll() = update {
        it.copy(assets = emptyList(), liab = emptyList(), view = View.DASHBOARD, catSel = null)
    }

    fun csv(): String = _state.value.let {
        buildCsv(it.assets, it.liab, it.members, it.included, it.rates)
    }

    fun saveMember(member: Member) = update { s ->
        val withId = if (member.id.isBlank()) member.copy(id = newId("m")) else member
        val members =
            if (s.members.any { it.id == withId.id }) s.members.map { if (it.id == withId.id) withId else it }
            else s.members + withId
        s.copy(members = members, included = s.included + (withId.id to (s.included[withId.id] ?: true)))
    }

    /** Remove a member (never "self"); their entries move to "self". */
    fun removeMember(id: String) = update { s ->
        if (id == "self") return@update s
        s.copy(
            members = s.members.filterNot { it.id == id },
            assets = s.assets.map { if (it.owner == id) it.copy(owner = "self") else it },
            liab = s.liab.map { if (it.owner == id) it.copy(owner = "self") else it },
            included = s.included - id,
        )
    }

    // ---- internals ----
    private fun setEphemeral(transform: (UiState) -> UiState) {
        _state.value = transform(_state.value)
    }

    private fun update(transform: (UiState) -> UiState) {
        val next = transform(_state.value).recordIfNonEmpty().copy(untouched = false)
        _state.value = next
        viewModelScope.launch {
            store.markTouched()
            store.save(next.toSnapshotData())
        }
        schedulePush() // debounced cloud push when signed in
    }

    /** Upsert this month's net-worth snapshot, unless there's nothing to track. */
    private fun UiState.recordIfNonEmpty(): UiState {
        if (assets.isEmpty() && liab.isEmpty()) return this
        val nw = netWorthOf(assets, liab, included, rates)
        return copy(history = recordSnapshot(history, currentMonth, nw))
    }

    private fun UiState.mapList(kind: Kind, transform: (List<Item>) -> List<Item>): UiState =
        if (kind == Kind.ASSET) copy(assets = transform(assets)) else copy(liab = transform(liab))

    private fun normalize(data: SnapshotData): Pair<List<Member>, Map<String, Boolean>> {
        val members = data.members.toMutableList()
        if (members.none { it.id == "self" }) {
            members.add(0, Member("self", "You", "Self", "#D5B475"))
        }
        val included = data.included.toMutableMap()
        members.forEach { if (included[it.id] == null) included[it.id] = true }
        return members to included
    }

    private fun currentMonth(): String = YearMonth.now().toString() // "2026-08"

    private var idCounter = 0L
    private fun newId(prefix: String): String = prefix + System.currentTimeMillis() + (idCounter++)
}
