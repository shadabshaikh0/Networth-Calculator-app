package com.shadabshaikh.networth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.domain.CatRow
import com.shadabshaikh.networth.domain.Derived
import com.shadabshaikh.networth.model.Kind
import com.shadabshaikh.networth.ui.NetworthViewModel
import com.shadabshaikh.networth.ui.components.DonutChart
import com.shadabshaikh.networth.ui.components.TrendLineChart
import com.shadabshaikh.networth.ui.theme.hexToColor
import com.shadabshaikh.networth.ui.theme.nwColors
import com.shadabshaikh.networth.ui.theme.tintFor

@Composable
fun DashboardScreen(d: Derived, vm: NetworthViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HeroCard(d)
        if (d.showOnboard) OnboardCard(d, vm)
        if (d.memberCards.size > 1) HouseholdCard(d, vm)
        AssetsVsLiabilitiesCard(d)
        if (d.donutSegs.isNotEmpty()) AllocationCard(d)
        LiquidityCard(d)
        if (d.hasTrend) TrendCard(d)
        if (d.assetCatRows.isNotEmpty()) {
            CategorySection("Assets", d.totalAssetsCompact, d.assetCatRows, vm)
        }
        if (d.liabCatRows.isNotEmpty()) {
            CategorySection("Liabilities", d.totalLiabCompact, d.liabCatRows, vm)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AddButton("＋ Add asset", nwColors.green, Modifier.weight(1f)) { vm.openAdd(Kind.ASSET) }
            AddButton("＋ Add liability", nwColors.red, Modifier.weight(1f)) { vm.openAdd(Kind.LIABILITY) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AddButton(label: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = nwColors.card,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, nwColors.cardBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun HeroCard(d: Derived) {
    NwCard {
        Column(Modifier.padding(20.dp)) {
            Text("Total net worth", color = nwColors.text3, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                d.nwCompact,
                color = if (d.nw >= 0) nwColors.gold else nwColors.red,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
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
private fun AssetsVsLiabilitiesCard(d: Derived) {
    NwCard {
        Column(Modifier.padding(20.dp)) {
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
        Box(
            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(nwColors.track),
        ) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(10.dp)
                    .clip(RoundedCornerShape(50)).background(color),
            )
        }
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
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(tintFor(color)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
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
                        onCheckedChange = { vm.toggleMember(m.id) },
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
private fun AllocationCard(d: Derived) {
    NwCard {
        Column(Modifier.padding(20.dp)) {
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
private fun LiquidityCard(d: Derived) {
    NwCard {
        Column(Modifier.padding(20.dp)) {
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
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(nwColors.track)) {
                    Box(
                        Modifier.fillMaxWidth(row.fraction.coerceIn(0f, 1f)).height(10.dp)
                            .clip(RoundedCornerShape(50)).background(hexToColor(row.colorHex)),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(d.emergencyLabel, color = nwColors.text3, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun TrendCard(d: Derived) {
    NwCard {
        Column(Modifier.padding(20.dp)) {
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
