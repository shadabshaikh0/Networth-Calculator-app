package com.shadabshaikh.networth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.domain.Derived
import com.shadabshaikh.networth.domain.HistRow
import com.shadabshaikh.networth.ui.NetworthViewModel
import com.shadabshaikh.networth.ui.components.TrendLineChart
import com.shadabshaikh.networth.ui.theme.nwColors

@Composable
fun HistoryScreen(d: Derived, vm: NetworthViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!d.hasTrend) {
            NwCard {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Not enough history yet", color = nwColors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your net worth is recorded automatically each month. Come back next month to see your trend and monthly changes.",
                        color = nwColors.text3, fontSize = 13.sp,
                    )
                }
            }
            return@Column
        }

        NwCard {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Net worth trend", color = nwColors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        d.trendGrowthLabel,
                        color = if (d.trendGrowthLabel.startsWith("▲")) nwColors.green else nwColors.red,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(16.dp))
                TrendLineChart(d.trendPoints, nwColors.gold, Modifier.fillMaxWidth().height(200.dp))
                Spacer(Modifier.height(8.dp))
                Row {
                    Text(d.firstMonthLabel, color = nwColors.text3, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(d.trendPoints.last().label, color = nwColors.text3, fontSize = 11.sp)
                }
            }
        }

        NwCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Monthly", color = nwColors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                d.histRows.reversed().forEach { row -> HistRowItem(row) }
            }
        }
    }
}

@Composable
private fun HistRowItem(row: HistRow) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.label, color = nwColors.text2, fontSize = 13.sp, modifier = Modifier.width(56.dp))
        Box(
            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)).background(nwColors.track),
        ) {
            Box(
                Modifier.fillMaxWidth(row.barFraction.coerceIn(0f, 1f)).height(8.dp)
                    .clip(RoundedCornerShape(50)).background(nwColors.gold),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(row.valueCompact, color = nwColors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(74.dp))
        Text(
            row.deltaLabel,
            color = when (row.deltaSign) { 1 -> nwColors.green; -1 -> nwColors.red; else -> nwColors.muted },
            fontSize = 12.sp,
            modifier = Modifier.width(64.dp),
        )
    }
}
