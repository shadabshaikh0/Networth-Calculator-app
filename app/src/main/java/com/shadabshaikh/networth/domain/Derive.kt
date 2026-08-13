package com.shadabshaikh.networth.domain

import com.shadabshaikh.networth.data.ASSET_CATS
import com.shadabshaikh.networth.data.DEFAULT_RATES
import com.shadabshaikh.networth.data.LIAB_CATS
import com.shadabshaikh.networth.data.LIQUID_CATS
import com.shadabshaikh.networth.model.CatSel
import com.shadabshaikh.networth.model.CategoryDef
import com.shadabshaikh.networth.model.Item
import com.shadabshaikh.networth.model.Kind
import com.shadabshaikh.networth.model.Member
import com.shadabshaikh.networth.model.Metal
import com.shadabshaikh.networth.model.Snapshot
import kotlin.math.abs

/**
 * Everything `derive()` needs, decoupled from the UI's UiState so this stays a
 * pure, testable domain function (no Android, no Compose).
 */
data class DeriveInput(
    val assets: List<Item>,
    val liab: List<Item>,
    val members: List<Member>,
    val included: Map<String, Boolean>,
    val rates: Map<String, Long>,
    val history: List<Snapshot>,
    val currentMonth: String,
    val onboardDismissed: Boolean,
    val catSel: CatSel?,
)

/** One slice of the asset-allocation donut. */
data class DonutSeg(
    val colorHex: String,
    val fraction: Float,
    val label: String,
    val pctLabel: String,
)

/** A liquid or locked row in the liquidity breakdown. */
data class LiqRow(
    val label: String,
    val colorHex: String,
    val valueCompact: String,
    val pctLabel: String,
    val fraction: Float,
)

/** One row in the monthly history table. */
data class HistRow(
    val label: String,
    val valueCompact: String,
    val barFraction: Float,
    val deltaLabel: String,
    val deltaSign: Int, // -1, 0, +1
)

/** A point on the net-worth trend line. */
data class TrendPoint(
    val month: String,
    val label: String,
    val value: Long,
)

/** One step in the onboarding checklist. */
data class ObStep(val label: String, val done: Boolean, val num: Int)

/** One member's summary card in the household section. */
data class MemberCard(
    val id: String,
    val name: String,
    val relation: String,
    val colorHex: String,
    val initial: String,
    val nwCompact: String,
    val included: Boolean,
    val itemCountLabel: String,
)

/** One item row inside the category drilldown. */
data class DrillItem(
    val id: String,
    val name: String,
    val valueFull: String,
    val barFraction: Float,
    val note: String?,
    val ref: String?,
    val weightLabel: String?,
    val hidden: Boolean,
    val excluded: Boolean,
    val excludedLabel: String,
    val ownerInitial: String,
    val ownerName: String,
    val ownerColorHex: String,
    val dimmed: Boolean,
)

/** The drilled-into category screen's data. */
data class Drill(
    val key: String,
    val kind: Kind,
    val label: String,
    val colorHex: String,
    val iconPath: String,
    val kindLabel: String,
    val count: Int,
    val totalFull: String,
    val pctLabel: String,
    val hasHidden: Boolean,
    val hiddenNote: String,
    val isEmpty: Boolean,
    val items: List<DrillItem>,
)

/** One category row on the dashboard (asset or liability). */
data class CatRow(
    val key: String,
    val kind: Kind,
    val colorHex: String,
    val label: String,
    val iconPath: String,
    val countLabel: String,
    val totalCompact: String,
    val pctLabel: String,
    val fraction: Float, // share of its side's total, for the bar width
)

/**
 * Display-ready values computed from raw state — the Kotlin port of the web's
 * `derive.ts` (numbers only; styling is done natively in Compose).
 */
data class Derived(
    val nw: Long,
    val nwCompact: String,
    val nwFull: String,
    val totalAssets: Long,
    val totalLiab: Long,
    val totalAssetsCompact: String,
    val totalLiabCompact: String,
    val assetsFull: String,
    val liabFull: String,
    val assetCount: Int,
    val liabCount: Int,
    val isEmpty: Boolean,
    // assets-vs-liabilities bars
    val avlAssetFraction: Float,
    val avlLiabFraction: Float,
    val leverageLabel: String,
    // category rows
    val assetCatRows: List<CatRow>,
    val liabCatRows: List<CatRow>,
    // asset-allocation donut
    val donutSegs: List<DonutSeg>,
    // liquid vs locked
    val liquidPctLabel: String,
    val emergencyLabel: String,
    val liqBreakdown: List<LiqRow>,
    // net-worth trend
    val trendPoints: List<TrendPoint>,
    val hasTrend: Boolean,
    val trendGrowthLabel: String,
    val firstMonthLabel: String,
    val histRows: List<HistRow>,
    // "since <month>" delta (hidden until 2+ monthly snapshots)
    val hasDelta: Boolean,
    val deltaLabel: String,
    val deltaPositive: Boolean,
    // household members
    val memberCards: List<MemberCard>,
    val householdLabel: String,
    // onboarding
    val showOnboard: Boolean,
    val onboardSteps: List<ObStep>,
    val onboardProgressLabel: String,
    // category drilldown (non-null only when a category is open)
    val drill: Drill?,
)

private data class Group(
    val def: CategoryDef,
    val total: Long,
    val visibleCount: Int,
    val hiddenCount: Int,
)

fun derive(input: DeriveInput): Derived {
    val rates = DEFAULT_RATES + input.rates
    // Price precious metals by weight, then work with resolved values.
    val assets = input.assets.map { it.copy(value = resolveValue(it, rates)) }
    val liab = input.liab.map { it.copy(value = resolveValue(it, rates)) }

    fun includedOwner(i: Item): Boolean = input.included[i.owner] != false
    val visibleAssets = assets.filter { !it.hidden && includedOwner(it) }
    val visibleLiab = liab.filter { !it.hidden && includedOwner(it) }

    val totalAssets = visibleAssets.sumOf { it.value }
    val totalLiab = visibleLiab.sumOf { it.value }
    val nw = totalAssets - totalLiab

    // ---- category grouping (mirrors the web's groupBy) ----
    fun groupsOf(items: List<Item>, cats: List<CategoryDef>): List<Group> =
        cats.mapNotNull { c ->
            val inCat = items.filter { it.cat == c.key && includedOwner(it) }
            if (inCat.isEmpty()) return@mapNotNull null
            val visible = inCat.filter { !it.hidden }
            Group(c, visible.sumOf { it.value }, visible.size, inCat.size - visible.size)
        }

    fun rowsOf(groups: List<Group>, grandTotal: Long, kind: Kind): List<CatRow> =
        groups.map { g ->
            val countLabel = buildString {
                append(if (g.visibleCount > 0) "${g.visibleCount} ${if (g.visibleCount == 1) "item" else "items"}" else "All hidden")
                if (g.hiddenCount > 0 && g.visibleCount > 0) append(" · ${g.hiddenCount} hidden")
            }
            CatRow(
                key = g.def.key, kind = kind, colorHex = g.def.color, label = g.def.label,
                iconPath = g.def.iconPath, countLabel = countLabel, totalCompact = compact(g.total),
                pctLabel = "${if (grandTotal > 0) Math.round(g.total * 100.0 / grandTotal) else 0}%",
                fraction = if (grandTotal > 0) (g.total.toFloat() / grandTotal) else 0f,
            )
        }.sortedByDescending { it.fraction }

    val assetGroups = groupsOf(assets, ASSET_CATS)
    val assetCatRows = rowsOf(assetGroups, totalAssets, Kind.ASSET)
    val liabCatRows = rowsOf(groupsOf(liab, LIAB_CATS), totalLiab, Kind.LIABILITY)

    // ---- asset-allocation donut (in category order, like the web) ----
    val donutSegs = assetGroups.map { g ->
        DonutSeg(
            colorHex = g.def.color,
            fraction = if (totalAssets > 0) g.total.toFloat() / totalAssets else 0f,
            label = g.def.label,
            pctLabel = "${if (totalAssets > 0) Math.round(g.total * 100.0 / totalAssets) else 0}%",
        )
    }

    // ---- liquid vs locked ----
    val liquidTotal = visibleAssets.filter { LIQUID_CATS.contains(it.cat) }.sumOf { it.value }
    val lockedTotal = totalAssets - liquidTotal
    val liqPct = if (totalAssets > 0) Math.round(liquidTotal * 100.0 / totalAssets).toInt() else 0
    val emergencyLabel = when {
        totalLiab > 0 && liquidTotal >= totalLiab -> "Covers all your debts if needed."
        totalLiab > 0 -> "Covers ${Math.round(liquidTotal * 100.0 / totalLiab)}% of your debts."
        else -> "Reachable without selling property or breaking locked savings."
    }
    val liqBreakdown = listOf(
        LiqRow("Liquid — reachable now", "#8BF1A7", compact(liquidTotal), "$liqPct%",
            if (totalAssets > 0) liquidTotal.toFloat() / totalAssets else 0f),
        LiqRow("Locked — tied up", "#A964F7", compact(lockedTotal), "${100 - liqPct}%",
            if (totalAssets > 0) lockedTotal.toFloat() / totalAssets else 0f),
    )

    // ---- assets vs liabilities bars ----
    val maxSide = maxOf(totalAssets, totalLiab, 1L).toFloat()
    val leverageLabel =
        if (totalLiab == 0L) "Debt-free — every rupee is yours."
        else "You owe ${Math.round(totalLiab * 100.0 / totalAssets)}% of what you own."

    // ---- monthly delta ----
    val past = input.history.filter { it.month != input.currentMonth }
    val points = past + Snapshot(input.currentMonth, nw)
    val hasTrend = points.size >= 2
    val spanYears = points.first().month.take(4) != points.last().month.take(4)
    val prev = if (hasTrend) points[points.size - 2] else null
    val monthDelta = if (prev != null) nw - prev.value else 0L
    val firstValue = points.first().value
    val growthPct = if (hasTrend && firstValue != 0L) (nw - firstValue) * 100.0 / abs(firstValue) else 0.0
    val trendPoints = points.map { TrendPoint(it.month, monthLabel(it.month, spanYears), it.value) }
    val hMax = points.maxOf { it.value }
    val histRows = points.mapIndexed { i, p ->
        val prevValue = if (i > 0) points[i - 1].value else p.value
        val delta = p.value - prevValue
        HistRow(
            label = monthLabel(p.month, spanYears),
            valueCompact = compact(p.value),
            barFraction = if (hMax > 0) maxOf(0.04f, p.value.toFloat() / hMax) else 0.04f,
            deltaLabel = if (i == 0) "—" else (if (delta >= 0) "+" else "") + compact(delta).replace("₹", ""),
            deltaSign = if (i == 0) 0 else if (delta >= 0) 1 else -1,
        )
    }

    // ---- household members ----
    val memberCards = input.members.map { mb ->
        val ma = assets.filter { it.owner == mb.id && !it.hidden }.sumOf { it.value }
        val ml = liab.filter { it.owner == mb.id && !it.hidden }.sumOf { it.value }
        val itemCount = assets.count { it.owner == mb.id } + liab.count { it.owner == mb.id }
        MemberCard(
            id = mb.id, name = mb.name, relation = mb.relation, colorHex = mb.color,
            initial = mb.name.take(1).uppercase(), nwCompact = compact(ma - ml),
            included = input.included[mb.id] != false,
            itemCountLabel = "$itemCount ${if (itemCount == 1) "entry" else "entries"}",
        )
    }
    val includedCount = input.members.count { input.included[it.id] != false }
    val householdLabel = if (includedCount == input.members.size) "Whole household"
    else "$includedCount of ${input.members.size} included"

    // ---- onboarding checklist ----
    val nonSelfMembers = input.members.count { it.id != "self" }
    val hasNote = (input.assets + input.liab).any { it.note != null }
    val obSteps = listOf(
        ObStep("Add your first asset", input.assets.isNotEmpty(), 1),
        ObStep("Add a liability you owe", input.liab.isNotEmpty(), 2),
        ObStep("Add a family member", nonSelfMembers > 0, 3),
        ObStep("Add a note to any entry", hasNote, 4),
    )
    val obDone = obSteps.count { it.done }
    val showOnboard = !input.onboardDismissed && obDone < obSteps.size

    // ---- category drilldown ----
    val drill = input.catSel?.let { sel ->
        val cats = if (sel.kind == Kind.ASSET) ASSET_CATS else LIAB_CATS
        val meta = cats.firstOrNull { it.key == sel.key } ?: cats.last()
        val src = if (sel.kind == Kind.ASSET) assets else liab
        fun excluded(i: Item) = input.included[i.owner] == false
        val items = src.filter { it.cat == sel.key }
            .sortedWith(compareBy({ if (it.hidden || excluded(it)) 1 else 0 }, { -it.value }))
        val visItems = items.filter { !it.hidden && !excluded(it) }
        val total = visItems.sumOf { it.value }
        val hiddenN = items.size - visItems.size
        val grand = if (sel.kind == Kind.ASSET) totalAssets else totalLiab
        val maxItem = maxOf(items.maxOfOrNull { it.value } ?: 0L, 1L)
        Drill(
            key = sel.key, kind = sel.kind, label = meta.label, colorHex = meta.color, iconPath = meta.iconPath,
            kindLabel = if (sel.kind == Kind.ASSET) "assets" else "liabilities",
            count = visItems.size, totalFull = inr(total),
            pctLabel = "${if (grand > 0) Math.round(total * 100.0 / grand) else 0}%",
            hasHidden = hiddenN > 0,
            hiddenNote = if (hiddenN > 0) "$hiddenN item${if (hiddenN > 1) "s" else ""} excluded from totals" else "",
            isEmpty = items.isEmpty(),
            items = items.map { item ->
                val owner = input.members.firstOrNull { it.id == item.owner } ?: input.members.firstOrNull()
                val excl = excluded(item)
                DrillItem(
                    id = item.id, name = item.name, valueFull = inr(item.value),
                    barFraction = maxOf(0.06f, item.value.toFloat() / maxItem),
                    note = item.note, ref = item.ref,
                    weightLabel = item.grams?.let { g ->
                        val gStr = if (g % 1.0 == 0.0) g.toLong().toString() else g.toString()
                        "$gStr g ${if (item.metal == Metal.SILVER) "silver" else "gold"} · ₹${rates[item.metal?.key ?: "gold"]}/g"
                    },
                    hidden = item.hidden, excluded = excl,
                    excludedLabel = "${owner?.name ?: ""} excluded",
                    ownerInitial = (owner?.name ?: "?").take(1).uppercase(),
                    ownerName = owner?.name ?: "", ownerColorHex = owner?.color ?: "#D5B475",
                    dimmed = item.hidden || excl,
                )
            },
        )
    }

    return Derived(
        nw = nw,
        nwCompact = compact(nw),
        nwFull = inr(nw),
        totalAssets = totalAssets,
        totalLiab = totalLiab,
        totalAssetsCompact = compact(totalAssets),
        totalLiabCompact = compact(totalLiab),
        assetsFull = inr(totalAssets),
        liabFull = inr(totalLiab),
        assetCount = visibleAssets.size,
        liabCount = visibleLiab.size,
        isEmpty = input.assets.isEmpty() && input.liab.isEmpty(),
        avlAssetFraction = totalAssets / maxSide,
        avlLiabFraction = totalLiab / maxSide,
        leverageLabel = leverageLabel,
        assetCatRows = assetCatRows,
        liabCatRows = liabCatRows,
        donutSegs = donutSegs,
        liquidPctLabel = "$liqPct%",
        emergencyLabel = emergencyLabel,
        liqBreakdown = liqBreakdown,
        trendPoints = trendPoints,
        hasTrend = hasTrend,
        trendGrowthLabel = (if (growthPct >= 0) "▲ " else "▼ ") + "%.1f".format(abs(growthPct)) + "%",
        firstMonthLabel = monthLabel(points.first().month, spanYears),
        histRows = histRows,
        hasDelta = prev != null,
        deltaLabel = if (prev != null) "${compact(abs(monthDelta))} since ${monthLabel(prev.month, spanYears)}" else "",
        deltaPositive = monthDelta >= 0,
        memberCards = memberCards,
        householdLabel = householdLabel,
        showOnboard = showOnboard,
        onboardSteps = obSteps,
        onboardProgressLabel = "$obDone of ${obSteps.size} done",
        drill = drill,
    )
}
