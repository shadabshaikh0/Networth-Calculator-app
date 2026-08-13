package com.shadabshaikh.networth.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.domain.CatRow
import com.shadabshaikh.networth.domain.Derived
import com.shadabshaikh.networth.model.Kind
import com.shadabshaikh.networth.ui.NetworthViewModel
import com.shadabshaikh.networth.ui.UiState
import com.shadabshaikh.networth.ui.components.AnimatedBar
import com.shadabshaikh.networth.ui.components.CategoryIcon
import com.shadabshaikh.networth.ui.components.DonutChart
import com.shadabshaikh.networth.ui.components.TrendLineChart
import com.shadabshaikh.networth.ui.theme.NwType
import com.shadabshaikh.networth.ui.theme.hexToColor
import com.shadabshaikh.networth.ui.theme.nwColors
import com.shadabshaikh.networth.ui.theme.tintFor

@Composable
fun DashboardScreen(state: UiState, d: Derived, vm: NetworthViewModel) {
    if (d.isEmpty) {
        EmptyState(vm)
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.untouched) SampleBanner(vm)
        HeroCard(d)
        if (d.showOnboard) OnboardCard(d, vm)
        ChartsCarousel(d)
        if (d.memberCards.size > 1) HouseholdCard(d, vm)
        if (d.assetCatRows.isNotEmpty()) {
            CategorySection("Assets", d.totalAssetsCompact, d.assetCatRows, vm)
        }
        if (d.liabCatRows.isNotEmpty()) {
            CategorySection("Liabilities", d.totalLiabCompact, d.liabCatRows, vm)
        }
        Spacer(Modifier.height(80.dp)) // clears the FAB + bottom nav
    }
}

@Composable
private fun EmptyState(vm: NetworthViewModel) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(tintFor(nwColors.gold)),
            contentAlignment = Alignment.Center,
        ) {
            Text("₹", color = nwColors.gold, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text("Start tracking your net worth", style = NwType.title, color = nwColors.text)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add what you own and what you owe to see your net worth, allocation, and trend over time.",
            color = nwColors.text3, fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        BigButton("＋ Add your first asset", nwColors.green, Color(0xFF0B0B0B)) { vm.openAdd(Kind.ASSET) }
        Spacer(Modifier.height(10.dp))
        BigButton("Load sample data", nwColors.inputBg, nwColors.text2, border = nwColors.chipBorder) { vm.loadSample() }
    }
}

@Composable
private fun BigButton(label: String, bg: Color, fg: Color, border: Color? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(50),
        border = border?.let { androidx.compose.foundation.BorderStroke(1.dp, it) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 15.dp),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SampleBanner(vm: NetworthViewModel) {
    Surface(
        color = tintFor(nwColors.gold),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Viewing sample data", color = nwColors.gold, style = NwType.captionStrong, modifier = Modifier.weight(1f))
            Text(
                "Start fresh",
                color = nwColors.gold, style = NwType.captionStrong,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.clearAll() }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Swipe-through carousel of the chart cards, so they don't stack vertically. */
@Composable
private fun ChartsCarousel(d: Derived) {
    val pages: List<@Composable (Modifier) -> Unit> = buildList {
        add { m -> AssetsVsLiabilitiesCard(d, m) }
        if (d.donutSegs.isNotEmpty()) add { m -> AllocationCard(d, m) }
        add { m -> LiquidityCard(d, m) }
        if (d.hasTrend) add { m -> TrendCard(d, m) }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(end = 34.dp), // peek of the next card
        ) { page ->
            pages[page](Modifier.fillMaxWidth().height(300.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                val active = pagerState.currentPage == i
                Box(
                    Modifier.padding(horizontal = 3.dp)
                        .size(if (active) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (active) nwColors.gold else nwColors.track),
                )
            }
        }
    }
}

@Composable
private fun HeroCard(d: Derived) {
    NwCard {
        Column(Modifier.padding(20.dp)) {
            Text("Total net worth", color = nwColors.text3, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = d.nwCompact,
                transitionSpec = {
                    (fadeIn(tween(250)) + slideInVertically { it / 3 }) togetherWith
                        (fadeOut(tween(200)) + slideOutVertically { -it / 3 })
                },
                label = "networth",
            ) { value ->
                Text(
                    value,
                    color = if (d.nw >= 0) nwColors.gold else nwColors.red,
                    style = NwType.display,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(d.nwFull, color = nwColors.text2, fontSize = 14.sp)
            if (d.hasDelta) {
                Spacer(Modifier.height(12.dp))
                val accent = if (d.deltaPositive) nwColors.green else nwColors.red
                Surface(color = tintFor(accent, 0.18f), shape = RoundedCornerShape(50)) {
                    Text(
                        (if (d.deltaPositive) "▲ " else "▼ ") + d.deltaLabel,
                        color = accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetsVsLiabilitiesCard(d: Derived, modifier: Modifier = Modifier) {
    NwCard(modifier) {
        Column(Modifier.fillMaxHeight().padding(20.dp), verticalArrangement = Arrangement.Center) {
            Text("Assets vs liabilities", color = nwColors.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            BarRow("Assets", d.assetsFull, d.avlAssetFraction, nwColors.green)
            Spacer(Modifier.height(12.dp))
            BarRow("Liabilities", d.liabFull, d.avlLiabFraction, nwColors.red)
            Spacer(Modifier.height(14.dp))
            Text(d.leverageLabel, color = nwColors.text3, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun BarRow(label: String, value: String, fraction: Float, color: Color) {
    Column {
        Row {
            Text(label, color = nwColors.text2, fontSize = 12.5.sp)
            Spacer(Modifier.weight(1f))
            Text(value, color = nwColors.text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        AnimatedBar(fraction = fraction, color = color, track = nwColors.track)
    }
}

@Composable
private fun CategorySection(title: String, total: String, rows: List<CatRow>, vm: NetworthViewModel) {
    NwCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = nwColors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(total, color = nwColors.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            rows.forEach { row ->
                CatRowItem(row) { vm.openCategory(row.kind, row.key) }
            }
        }
    }
}

@Composable
private fun CatRowItem(row: CatRow, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = hexToColor(row.colorHex)
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(tintFor(color)),
            contentAlignment = Alignment.Center,
        ) {
            CategoryIcon(row.iconPath, color, Modifier.size(22.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.label, color = nwColors.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
            Text(row.countLabel, color = nwColors.text3, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(row.totalCompact, color = nwColors.text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            Text(row.pctLabel, color = nwColors.text3, fontSize = 12.sp)
        }
    }
}

@Composable
private fun OnboardCard(d: Derived, vm: NetworthViewModel) {
    NwCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Get started", color = nwColors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(d.onboardProgressLabel, color = nwColors.text3, fontSize = 12.5.sp)
                }
                Text(
                    "Dismiss",
                    color = nwColors.text3, fontSize = 12.5.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.dismissOnboard() }.padding(6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            d.onboardSteps.forEach { step ->
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape)
                            .background(if (step.done) hexToColor("#19AA4D") else Color.Transparent)
                            .then(if (step.done) Modifier else Modifier.border(1.5.dp, nwColors.chipBorder, CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (step.done) "✓" else step.num.toString(),
                            color = if (step.done) Color.White else nwColors.text3,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        step.label,
                        color = if (step.done) nwColors.text3 else nwColors.text,
                        fontSize = 14.sp,
                        textDecoration = if (step.done) TextDecoration.LineThrough else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun HouseholdCard(d: Derived, vm: NetworthViewModel) {
    val haptic = LocalHapticFeedback.current
    NwCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Household", color = nwColors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(d.householdLabel, color = nwColors.text3, fontSize = 12.5.sp)
                }
                Text(
                    "Manage",
                    color = nwColors.gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.openMembers() }.padding(6.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            d.memberCards.forEach { m ->
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val alpha = if (m.included) 1f else 0.45f
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(hexToColor(m.colorHex).copy(alpha = alpha)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(m.initial, color = Color(0xFF0B0B0B), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${m.name} · ${m.relation}", color = nwColors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${m.itemCountLabel} · ${m.nwCompact}", color = nwColors.text3, fontSize = 12.sp)
                    }
                    Switch(
                        checked = m.included,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.toggleMember(m.id)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = hexToColor(m.colorHex),
                            checkedBorderColor = hexToColor(m.colorHex),
                            uncheckedThumbColor = nwColors.text3,
                            uncheckedTrackColor = nwColors.track,
                            uncheckedBorderColor = nwColors.chipBorder,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AllocationCard(d: Derived, modifier: Modifier = Modifier) {
    NwCard(modifier) {
        Column(Modifier.fillMaxHeight().padding(20.dp), verticalArrangement = Arrangement.Center) {
            Text("Asset allocation", color = nwColors.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutChart(
                    segs = d.donutSegs,
                    trackColor = nwColors.grid,
                    strokeWidth = 20.dp,
                    modifier = Modifier.size(130.dp),
                )
                Spacer(Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    d.donutSegs.forEach { seg ->
                        Row(
                            Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(hexToColor(seg.colorHex)))
                            Spacer(Modifier.size(8.dp))
                            Text(seg.label, color = nwColors.legend, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                            Text(seg.pctLabel, color = nwColors.text3, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidityCard(d: Derived, modifier: Modifier = Modifier) {
    NwCard(modifier) {
        Column(Modifier.fillMaxHeight().padding(20.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Liquid vs locked", color = nwColors.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(d.liquidPctLabel + " liquid", color = nwColors.green, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            d.liqBreakdown.forEachIndexed { i, row ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                Row {
                    Text(row.label, color = nwColors.text2, fontSize = 12.5.sp)
                    Spacer(Modifier.weight(1f))
                    Text(row.valueCompact, color = nwColors.text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                AnimatedBar(fraction = row.fraction, color = hexToColor(row.colorHex), track = nwColors.track)
            }
            Spacer(Modifier.height(14.dp))
            Text(d.emergencyLabel, color = nwColors.text3, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun TrendCard(d: Derived, modifier: Modifier = Modifier) {
    NwCard(modifier) {
        Column(Modifier.fillMaxHeight().padding(20.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Net worth trend", color = nwColors.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    d.trendGrowthLabel,
                    color = if (d.trendGrowthLabel.startsWith("▲")) nwColors.green else nwColors.red,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(14.dp))
            TrendLineChart(
                points = d.trendPoints,
                lineColor = nwColors.gold,
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Text(d.firstMonthLabel, color = nwColors.text3, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text(d.trendPoints.last().label, color = nwColors.text3, fontSize = 11.sp)
            }
        }
    }
}

/** Standard rounded card matching the web look: card bg + hairline border. */
@Composable
fun NwCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = nwColors.card,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, nwColors.cardBorder, RoundedCornerShape(16.dp)),
    ) { content() }
}
