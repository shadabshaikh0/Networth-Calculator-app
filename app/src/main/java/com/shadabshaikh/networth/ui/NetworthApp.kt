package com.shadabshaikh.networth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.model.View
import com.shadabshaikh.networth.ui.screens.CategoryDrilldownScreen
import com.shadabshaikh.networth.ui.screens.DashboardScreen
import com.shadabshaikh.networth.ui.screens.HistoryScreen
import com.shadabshaikh.networth.ui.sheets.AddEditItemSheet
import com.shadabshaikh.networth.ui.sheets.ManageMembersSheet
import com.shadabshaikh.networth.ui.theme.nwColors

@Composable
fun NetworthApp(vm: NetworthViewModel) {
    val state by vm.state.collectAsState()
    val derived by vm.derived.collectAsState()

    Surface(color = nwColors.bg, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(state = state, vm = vm)
            Box(Modifier.weight(1f)) {
                when (state.view) {
                    View.DASHBOARD -> DashboardScreen(derived, vm)
                    View.HISTORY -> HistoryScreen(derived, vm)
                    View.CATEGORY -> CategoryDrilldownScreen(derived, vm)
                }
            }
        }
    }
    AddEditItemSheet(state, vm)
    ManageMembersSheet(state, vm)
}

@Composable
private fun TopBar(state: UiState, vm: NetworthViewModel) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Net worth", color = nwColors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (state.theme == "light") Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = "Toggle theme",
                tint = nwColors.gold,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = vm::toggleTheme).padding(6.dp),
            )
            Spacer(Modifier.width(4.dp))
            Box {
                Icon(
                    Icons.Filled.MoreVert, contentDescription = "More",
                    tint = nwColors.text2,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { menuOpen = true }.padding(6.dp),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Export CSV") },
                        onClick = { menuOpen = false; shareCsv(context, vm.csv()) },
                    )
                    DropdownMenuItem(
                        text = { Text("Load sample data") },
                        onClick = { menuOpen = false; vm.loadSample() },
                    )
                    DropdownMenuItem(
                        text = { Text("Clear all") },
                        onClick = { menuOpen = false; confirmClear = true },
                    )
                }
            }
        }
        Row(Modifier.padding(top = 12.dp)) {
            Tab("Dashboard", active = state.view != View.HISTORY, onClick = vm::gotoDashboard)
            Spacer(Modifier.width(8.dp))
            Tab("History", active = state.view == View.HISTORY, onClick = vm::gotoHistory)
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all data?") },
            text = { Text("This removes every asset and liability. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearAll() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Tab(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (active) nwColors.ctaBg else nwColors.bg,
        contentColor = if (active) nwColors.ctaTx else nwColors.text2,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
