package com.shadabshaikh.networth.domain

import com.shadabshaikh.networth.model.Snapshot

/**
 * Upsert a net-worth snapshot for [month] (YYYY-MM). If the month already
 * exists with the same value, returns the list unchanged; otherwise inserts or
 * updates it and keeps the list sorted by month. Mirrors the web's
 * `recordSnapshot`. The caller decides whether to record at all (the web skips
 * while there are zero items).
 */
fun recordSnapshot(history: List<Snapshot>, month: String, value: Long): List<Snapshot> {
    val existing = history.firstOrNull { it.month == month }
    if (existing != null && existing.value == value) return history
    val updated = history.filter { it.month != month } + Snapshot(month, value)
    return updated.sortedBy { it.month }
}
