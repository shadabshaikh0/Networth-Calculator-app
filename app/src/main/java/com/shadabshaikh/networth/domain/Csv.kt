package com.shadabshaikh.networth.domain

import com.shadabshaikh.networth.data.ASSET_CATS
import com.shadabshaikh.networth.data.DEFAULT_RATES
import com.shadabshaikh.networth.data.LIAB_CATS
import com.shadabshaikh.networth.model.CategoryDef
import com.shadabshaikh.networth.model.Item
import com.shadabshaikh.networth.model.Member

/** Build the CSV export, mirroring the web app's `exportCSV` columns exactly. */
fun buildCsv(
    assets: List<Item>,
    liab: List<Item>,
    members: List<Member>,
    included: Map<String, Boolean>,
    rates: Map<String, Long>,
): String {
    val r = DEFAULT_RATES + rates
    fun ownerName(id: String) = members.firstOrNull { it.id == id }?.name ?: id
    fun esc(v: String) = if (v.contains(Regex("[\",\n]"))) "\"" + v.replace("\"", "\"\"") + "\"" else v

    val rows = mutableListOf(
        listOf("Type", "Name", "Category", "Owner", "Value (INR)", "Weight (g)", "Metal", "In totals", "Note", "Reference"),
    )
    fun push(kind: String, arr: List<Item>, cats: List<CategoryDef>) {
        arr.forEach { i ->
            val cat = cats.firstOrNull { it.key == i.cat }?.label ?: i.cat
            val inc = included[i.owner] != false && !i.hidden
            rows.add(
                listOf(
                    kind, i.name, cat, ownerName(i.owner), resolveValue(i, r).toString(),
                    i.grams?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "",
                    i.metal?.key ?: "", if (inc) "Yes" else "Excluded", i.note ?: "", i.ref ?: "",
                ),
            )
        }
    }
    push("Asset", assets, ASSET_CATS)
    push("Liability", liab, LIAB_CATS)
    return rows.joinToString("\r\n") { row -> row.joinToString(",") { esc(it) } }
}
