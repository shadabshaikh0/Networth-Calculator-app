package com.shadabshaikh.networth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.domain.Derived
import com.shadabshaikh.networth.domain.Drill
import com.shadabshaikh.networth.domain.DrillItem
import com.shadabshaikh.networth.ui.NetworthViewModel
import com.shadabshaikh.networth.ui.components.CategoryIcon
import com.shadabshaikh.networth.ui.theme.hexToColor
import com.shadabshaikh.networth.ui.theme.nwColors
import com.shadabshaikh.networth.ui.theme.tintFor

@Composable
fun CategoryDrilldownScreen(d: Derived, vm: NetworthViewModel) {
    val drill = d.drill ?: return
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { Header(drill, onBack = vm::gotoDashboard) }
        if (drill.hasHidden) {
            item {
                Text(drill.hiddenNote, color = nwColors.text3, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        items(drill.items, key = { it.id }) { item ->
            DrillRow(
                item = item,
                catColor = hexToColor(drill.colorHex),
                onToggleHidden = { vm.toggleHidden(drill.kind, item.id) },
                onClick = { vm.openEditById(drill.kind, item.id) },
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = nwColors.card,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, nwColors.cardBorder, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp)).clickable { vm.openAdd(drill.kind, drill.key) },
            ) {
                Text(
                    "＋ Add to ${drill.label}",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    color = hexToColor(drill.colorHex),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Header(drill: Drill, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = nwColors.text2,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(4.dp),
        )
        Spacer(Modifier.size(8.dp))
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(tintFor(hexToColor(drill.colorHex))),
            contentAlignment = Alignment.Center,
        ) {
            CategoryIcon(drill.iconPath, hexToColor(drill.colorHex), Modifier.size(22.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(drill.label, color = nwColors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("${drill.count} ${drill.kindLabel}", color = nwColors.text3, fontSize = 12.5.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(drill.totalFull, color = nwColors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${drill.pctLabel} of ${drill.kindLabel}", color = nwColors.text3, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DrillRow(item: DrillItem, catColor: Color, onToggleHidden: () -> Unit, onClick: () -> Unit) {
    val contentAlpha = if (item.dimmed) 0.4f else 1f
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(hexToColor(item.ownerColorHex).copy(alpha = contentAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.ownerInitial, color = Color(0xFF0B0B0B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, color = nwColors.text.copy(alpha = contentAlpha), fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                if (item.excluded) {
                    Spacer(Modifier.size(6.dp))
                    Text("· ${item.excludedLabel}", color = nwColors.text3, fontSize = 11.sp)
                }
            }
            item.weightLabel?.let { Text(it, color = nwColors.text3, fontSize = 11.5.sp) }
            item.note?.let { Text("“$it”", color = nwColors.text3, fontSize = 11.5.sp) }
            item.ref?.let { Text(it, color = nwColors.gold, fontSize = 11.5.sp) }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(nwColors.track)) {
                Box(
                    Modifier.fillMaxWidth(item.barFraction.coerceIn(0f, 1f)).height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (item.dimmed) nwColors.iconGrey else catColor),
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(item.valueFull, color = nwColors.text.copy(alpha = contentAlpha), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Icon(
                if (item.hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (item.hidden) "Show in totals" else "Hide from totals",
                tint = if (item.hidden) nwColors.gold else nwColors.text3,
                modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onToggleHidden).padding(4.dp),
            )
        }
    }
}
