package com.shadabshaikh.networth.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.data.MEMBER_COLORS
import com.shadabshaikh.networth.data.RELATIONS
import com.shadabshaikh.networth.model.Member
import com.shadabshaikh.networth.ui.NetworthViewModel
import com.shadabshaikh.networth.ui.UiState
import com.shadabshaikh.networth.ui.components.SelectChip
import com.shadabshaikh.networth.ui.theme.hexToColor
import com.shadabshaikh.networth.ui.theme.nwColors
import com.shadabshaikh.networth.ui.theme.tintFor

private data class MemberDraft(val id: String, val name: String, val relation: String, val color: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageMembersSheet(state: UiState, vm: NetworthViewModel) {
    if (!state.showMembers) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // "list" | "add" | "edit" — local to the sheet.
    var mode by remember { mutableStateOf("list") }
    var draft by remember { mutableStateOf(MemberDraft("", "", "Spouse", MEMBER_COLORS.first())) }

    fun entriesFor(id: String) = state.assets.count { it.owner == id } + state.liab.count { it.owner == id }

    ModalBottomSheet(
        onDismissRequest = vm::closeMembers,
        sheetState = sheetState,
        containerColor = nwColors.card,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (mode == "list") {
                Text("Manage household", color = nwColors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Add, edit, or remove people whose money you track together.",
                    color = nwColors.text3, fontSize = 12.5.sp)

                state.members.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable {
                                draft = MemberDraft(m.id, m.name, m.relation, m.color)
                                mode = "edit"
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(m.name.take(1).uppercase(), hexToColor(m.color), 38.dp)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, color = nwColors.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "${m.relation} · ${entriesFor(m.id)} ${if (entriesFor(m.id) == 1) "entry" else "entries"}",
                                color = nwColors.text3, fontSize = 12.sp,
                            )
                        }
                        if (m.id == "self") Text("You", color = nwColors.text3, fontSize = 12.sp)
                    }
                }

                Surface(
                    color = nwColors.inputBg, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                        val used = state.members.map { it.color }.toSet()
                        val next = MEMBER_COLORS.firstOrNull { it !in used } ?: MEMBER_COLORS.first()
                        draft = MemberDraft("", "", "Spouse", next)
                        mode = "add"
                    },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = nwColors.gold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Add family member", color = nwColors.gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                val isSelf = draft.id == "self"
                Text(if (mode == "add") "Add family member" else "Edit member",
                    color = nwColors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar((draft.name.ifBlank { "?" }).take(1).uppercase(), hexToColor(draft.color), 44.dp)
                    Spacer(Modifier.size(12.dp))
                    Text(draft.name.ifBlank { "New member" }, color = nwColors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                MemberField("Name", draft.name) { draft = draft.copy(name = it) }

                if (!isSelf) {
                    Text("Relation", color = nwColors.text3, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RELATIONS.forEach { r ->
                            SelectChip(r, selected = draft.relation == r) { draft = draft.copy(relation = r) }
                        }
                    }
                }

                Text("Colour", color = nwColors.text3, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MEMBER_COLORS.forEach { c ->
                        val selected = draft.color == c
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(hexToColor(c))
                                .border(2.dp, if (selected) nwColors.text else Color.Transparent, CircleShape)
                                .clickable { draft = draft.copy(color = c) },
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mode == "edit" && !isSelf) {
                        TextButton(onClick = { vm.removeMember(draft.id); mode = "list" }) {
                            Text("Remove", color = nwColors.red)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    val canSave = draft.name.trim().isNotEmpty()
                    Surface(
                        color = if (canSave) nwColors.ctaBg else nwColors.inputBg,
                        contentColor = if (canSave) nwColors.ctaTx else nwColors.text3,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            if (mode == "add") "Add member" else "Save",
                            modifier = Modifier
                                .let { if (canSave) it.clickable {
                                    vm.saveMember(Member(draft.id, draft.name.trim(), if (isSelf) "Self" else draft.relation, draft.color))
                                    mode = "list"
                                } else it }
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(initial: String, color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(initial, color = Color(0xFF0B0B0B), fontSize = (size.value * 0.4).sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = nwColors.inputBg,
            unfocusedContainerColor = nwColors.inputBg,
            focusedBorderColor = nwColors.gold,
            unfocusedBorderColor = nwColors.inputBorder,
            focusedTextColor = nwColors.text,
            unfocusedTextColor = nwColors.text,
            focusedLabelColor = nwColors.text3,
            unfocusedLabelColor = nwColors.text3,
            cursorColor = nwColors.gold,
        ),
    )
}
