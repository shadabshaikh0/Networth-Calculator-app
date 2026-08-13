package com.shadabshaikh.networth.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.ui.NetworthViewModel
import com.shadabshaikh.networth.ui.UiState
import com.shadabshaikh.networth.ui.theme.NwType
import com.shadabshaikh.networth.ui.theme.nwColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(state: UiState, vm: NetworthViewModel) {
    if (!state.showAccount) return
    val account = state.account
    val name = account?.name?.takeIf { it.isNotBlank() } ?: account?.email?.substringBefore("@") ?: "Account"
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = vm::closeAccount,
        sheetState = sheetState,
        containerColor = nwColors.card,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Account", style = NwType.title, color = nwColors.text)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(nwColors.gold),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initial, color = Color(0xFF0B0B0B), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = nwColors.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    account?.email?.let { Text(it, color = nwColors.text3, fontSize = 13.sp) }
                }
            }

            Surface(color = nwColors.inputBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(nwColors.green))
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Connected with Google", color = nwColors.text, style = NwType.captionStrong)
                        Text("Drive access for sheet sync", color = nwColors.text3, fontSize = 11.5.sp)
                    }
                }
            }

            HorizontalDivider(color = nwColors.hair)

            Surface(
                onClick = { vm.signOut() },
                color = tint(nwColors.red),
                contentColor = nwColors.red,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Sign out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun tint(c: Color) = c.copy(alpha = 0.14f)
