package com.shadabshaikh.networth.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.model.Kind
import com.shadabshaikh.networth.model.View
import com.shadabshaikh.networth.ui.screens.CategoryDrilldownScreen
import com.shadabshaikh.networth.ui.screens.DashboardScreen
import com.shadabshaikh.networth.ui.screens.HistoryScreen
import com.shadabshaikh.networth.ui.sheets.AccountSheet
import com.shadabshaikh.networth.ui.sheets.AddEditItemSheet
import com.shadabshaikh.networth.ui.sheets.ManageMembersSheet
import com.shadabshaikh.networth.ui.theme.nwColors
import com.shadabshaikh.networth.ui.theme.tintFor

@Composable
fun NetworthApp(vm: NetworthViewModel) {
    val state by vm.state.collectAsState()
    val derived by vm.derived.collectAsState()

    // Android back returns to the dashboard from any sub-screen.
    BackHandler(enabled = state.view != View.DASHBOARD) { vm.gotoDashboard() }

    Scaffold(
        containerColor = nwColors.bg,
        topBar = { TopBar(state, vm) },
        bottomBar = { BottomNav(state, vm) },
        floatingActionButton = { if (state.view != View.HISTORY) AddFab(vm) },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
            if (!state.loaded) {
                CircularProgressIndicator(color = nwColors.gold)
            } else when (state.view) {
                View.DASHBOARD -> DashboardScreen(state, derived, vm)
                View.HISTORY -> HistoryScreen(derived, vm)
                View.CATEGORY -> CategoryDrilldownScreen(derived, vm)
            }
        }
    }

    AddEditItemSheet(state, vm)
    ManageMembersSheet(state, vm)
    AccountSheet(state, vm)
}

@Composable
private fun TopBar(state: UiState, vm: NetworthViewModel) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                DropdownMenuItem(text = { Text("Export CSV") }, onClick = { menuOpen = false; shareCsv(context, vm.csv()) })
                DropdownMenuItem(text = { Text("Load sample data") }, onClick = { menuOpen = false; vm.loadSample() })
                DropdownMenuItem(text = { Text("Clear all") }, onClick = { menuOpen = false; confirmClear = true })
            }
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
private fun AddFab(vm: NetworthViewModel) {
    var open by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Box {
        ExtendedFloatingActionButton(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); open = true },
            containerColor = nwColors.ctaBg,
            contentColor = nwColors.ctaTx,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add") },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Add asset", color = nwColors.green) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = nwColors.green)
                },
                onClick = { open = false; vm.openAdd(Kind.ASSET) },
            )
            DropdownMenuItem(
                text = { Text("Add liability", color = nwColors.red) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = nwColors.red)
                },
                onClick = { open = false; vm.openAdd(Kind.LIABILITY) },
            )
        }
    }
}

@Composable
private fun BottomNav(state: UiState, vm: NetworthViewModel) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) vm.completeSignIn(result.data) else vm.cancelSignIn()
    }
    val signedIn = state.authStatus == AuthStatus.SIGNED_IN
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = nwColors.gold,
        selectedTextColor = nwColors.text,
        indicatorColor = tintFor(nwColors.gold),
        unselectedIconColor = nwColors.text3,
        unselectedTextColor = nwColors.text3,
    )

    NavigationBar(containerColor = nwColors.card, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = state.view != View.HISTORY,
            onClick = vm::gotoDashboard,
            icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = state.view == View.HISTORY,
            onClick = vm::gotoHistory,
            icon = { Icon(Icons.Filled.Timeline, contentDescription = "History") },
            label = { Text("History") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = state.showAccount,
            onClick = {
                when (state.authStatus) {
                    AuthStatus.SIGNED_IN -> vm.openAccount()
                    AuthStatus.SIGNED_OUT -> vm.beginSignIn(onNeedConsent = { pending ->
                        launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    })
                    AuthStatus.SIGNING_IN -> {}
                }
            },
            icon = {
                if (signedIn) {
                    val initial = (state.account?.name?.takeIf { it.isNotBlank() } ?: state.account?.email ?: "?")
                        .trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Box(
                        Modifier.size(26.dp).clip(RoundedCornerShape(50)).background(nwColors.gold),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initial, color = androidx.compose.ui.graphics.Color(0xFF0B0B0B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = "Sign in")
                }
            },
            label = {
                Text(
                    when (state.authStatus) {
                        AuthStatus.SIGNING_IN -> "Signing in…"
                        AuthStatus.SIGNED_IN ->
                            (state.account?.name?.substringBefore(" ")
                                ?: state.account?.email?.substringBefore("@") ?: "Account").take(12)
                        AuthStatus.SIGNED_OUT -> "Sign in"
                    },
                )
            },
            colors = itemColors,
        )
    }
}
