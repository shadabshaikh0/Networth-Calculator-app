package com.shadabshaikh.networth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Whether an item is something you own or something you owe. */
enum class Kind { ASSET, LIABILITY }

/** Precious metal an item can be priced by weight. Serializes to lowercase
 *  ("gold"/"silver") for cross-platform parity, and carries [key] for looking
 *  it up in the rates map. */
@Serializable
enum class Metal(val key: String) {
    @SerialName("gold") GOLD("gold"),
    @SerialName("silver") SILVER("silver"),
}

/** Which top-level screen is showing. */
enum class View { DASHBOARD, CATEGORY, HISTORY }

/** A single asset or liability. Money is whole rupees ([value]); precious
 *  metals may instead be priced from [grams] × current rate. Immutable — edit
 *  by producing a copy(). */
@Serializable
data class Item(
    val id: String,
    val name: String,
    val cat: String,
    val value: Long,
    val owner: String = "self",
    val hidden: Boolean = false,
    val note: String? = null,
    val ref: String? = null,
    val grams: Double? = null,
    val metal: Metal? = null,
)

/** A person whose money is tracked in the household. */
@Serializable
data class Member(
    val id: String,
    val name: String,
    val relation: String,
    val color: String,
)

/** One recorded net-worth data point, keyed by calendar month (YYYY-MM). */
@Serializable
data class Snapshot(
    val month: String,
    val value: Long,
)

/** The full set of user data we persist locally (and later sync to a Sheet). */
@Serializable
data class SnapshotData(
    val assets: List<Item> = emptyList(),
    val liab: List<Item> = emptyList(),
    val members: List<Member> = emptyList(),
    val included: Map<String, Boolean> = emptyMap(),
    val rates: Map<String, Long> = emptyMap(),
    val onboardDismissed: Boolean = false,
    val history: List<Snapshot> = emptyList(),
)

/** Static definition of a category (static config, not persisted). */
data class CategoryDef(
    val key: String,
    val label: String,
    val color: String,
    val iconPath: String,
)

/** The currently drilled-into category. */
data class CatSel(
    val kind: Kind,
    val key: String,
    val owner: String? = null,
)
