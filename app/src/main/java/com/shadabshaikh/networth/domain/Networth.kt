package com.shadabshaikh.networth.domain

import com.shadabshaikh.networth.data.DEFAULT_RATES
import com.shadabshaikh.networth.model.Item
import kotlin.math.roundToLong

/** An item's live value — precious metals priced by weight × current rate,
 *  otherwise the stored [Item.value]. Mirrors the web's `resolveValue`. */
fun resolveValue(item: Item, rates: Map<String, Long>): Long {
    val metal = item.metal
    val grams = item.grams
    if (grams != null && metal != null) {
        val rate = rates[metal.key]
        if (rate != null && rate != 0L) return (grams * rate).roundToLong()
    }
    return item.value
}

/** Net worth = visible, included assets − visible, included liabilities.
 *  "Included" = owner not explicitly excluded; "visible" = not hidden. */
fun netWorthOf(
    assets: List<Item>,
    liab: List<Item>,
    included: Map<String, Boolean>,
    rawRates: Map<String, Long>,
): Long {
    val rates = DEFAULT_RATES + rawRates // passed rates override the defaults
    fun isIncluded(i: Item): Boolean = included[i.owner] != false && !i.hidden
    val totalAssets = assets.filter(::isIncluded).sumOf { resolveValue(it, rates) }
    val totalLiab = liab.filter(::isIncluded).sumOf { resolveValue(it, rates) }
    return totalAssets - totalLiab
}

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Short label for a YYYY-MM month key, e.g. "Jul" or "Jul '26" across years. */
fun monthLabel(month: String, withYear: Boolean = false): String {
    val parts = month.split("-")
    val year = parts.getOrNull(0) ?: month
    val monthNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val name = MONTHS.getOrNull(monthNum - 1) ?: month
    return if (withYear) "$name '${year.takeLast(2)}" else name
}
