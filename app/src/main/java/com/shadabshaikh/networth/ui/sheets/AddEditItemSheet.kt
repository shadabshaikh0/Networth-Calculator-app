package com.shadabshaikh.networth.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.data.ASSET_CATS
import com.shadabshaikh.networth.data.DEFAULT_RATES
import com.shadabshaikh.networth.data.LIAB_CATS
import com.shadabshaikh.networth.domain.inr
import com.shadabshaikh.networth.model.Item
import com.shadabshaikh.networth.model.Kind
import com.shadabshaikh.networth.model.Metal
import com.shadabshaikh.networth.ui.NetworthViewModel
import com.shadabshaikh.networth.ui.UiState
import com.shadabshaikh.networth.ui.components.SelectChip
import com.shadabshaikh.networth.ui.theme.hexToColor
import com.shadabshaikh.networth.ui.theme.nwColors
import com.shadabshaikh.networth.ui.theme.tintFor
import kotlin.math.roundToLong

private data class Draft(
    val name: String,
    val cat: String,
    val valueStr: String,
    val owner: String,
    val note: String,
    val ref: String,
    val grams: String,
    val metal: Metal,
    val weightMode: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditItemSheet(state: UiState, vm: NetworthViewModel) {
    val target = state.editor ?: return
    val kind = target.kind
    val isAsset = kind == Kind.ASSET
    val cats = if (isAsset) ASSET_CATS else LIAB_CATS
    val isEdit = target.item?.id?.isNotBlank() == true

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Draft owned by the sheet; re-initialized whenever a different editor opens.
    var draft by remember(target) {
        val it = target.item
        mutableStateOf(
            Draft(
                name = it?.name ?: "",
                cat = it?.cat ?: cats.first().key,
                valueStr = if (it != null && it.id.isNotBlank() && it.grams == null) it.value.toString() else "",
                owner = it?.owner ?: "self",
                note = it?.note ?: "",
                ref = it?.ref ?: "",
                grams = it?.grams?.let { g -> if (g % 1.0 == 0.0) g.toLong().toString() else g.toString() } ?: "",
                metal = it?.metal ?: Metal.GOLD,
                weightMode = it?.grams != null,
            ),
        )
    }

    val rates = DEFAULT_RATES + state.rates
    val isGold = isAsset && draft.cat == "gold"
    val byWeight = isGold && draft.weightMode
    val grams = draft.grams.toDoubleOrNull() ?: 0.0
    val metalRate = rates[draft.metal.key] ?: 0L
    val value: Long = if (byWeight) (grams * metalRate).roundToLong() else draft.valueStr.toLongOrNull() ?: 0L
    val canSave = value > 0L

    ModalBottomSheet(
        onDismissRequest = vm::closeEditor,
        sheetState = sheetState,
        containerColor = nwColors.card,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (isEdit) "Edit ${if (isAsset) "asset" else "liability"}" else if (isAsset) "Add asset" else "Add liability",
                color = nwColors.text, fontSize = 19.sp, fontWeight = FontWeight.Bold,
            )

            // live value preview
            Text(if (value > 0) inr(value) else "₹0", color = if (isAsset) nwColors.green else nwColors.red,
                fontSize = 26.sp, fontWeight = FontWeight.Bold)

            Field("Name", draft.name, if (isAsset) "e.g. Zerodha equity" else "e.g. HDFC home loan") {
                draft = draft.copy(name = it)
            }

            Label("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.forEach { c ->
                    SelectChip(c.label, selected = draft.cat == c.key, accent = hexToColor(c.color)) {
                        draft = draft.copy(cat = c.key)
                    }
                }
            }

            if (isGold) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip("By value", selected = !draft.weightMode) { draft = draft.copy(weightMode = false) }
                    SelectChip("By weight", selected = draft.weightMode) { draft = draft.copy(weightMode = true) }
                }
            }

            if (byWeight) {
                Field("Weight (grams)", draft.grams, "e.g. 10", keyboard = KeyboardType.Decimal) {
                    draft = draft.copy(grams = it.filter { ch -> ch.isDigit() || ch == '.' })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip("Gold", selected = draft.metal == Metal.GOLD) { draft = draft.copy(metal = Metal.GOLD) }
                    SelectChip("Silver", selected = draft.metal == Metal.SILVER) { draft = draft.copy(metal = Metal.SILVER) }
                }
                Field("${if (draft.metal == Metal.SILVER) "Silver" else "Gold 24K"} rate ₹/g", metalRate.toString(), "", keyboard = KeyboardType.Number) {
                    val r = it.filter(Char::isDigit).toLongOrNull() ?: 0L
                    vm.setRate(draft.metal.key, r)
                }
                Text(
                    if (grams > 0) "$grams g × ₹$metalRate = ${inr(value)}" else "Enter weight to price it live",
                    color = nwColors.text3, fontSize = 12.5.sp,
                )
            } else {
                Field(if (isAsset) "Asset value" else "Amount owed", draft.valueStr, "0", keyboard = KeyboardType.Number) {
                    draft = draft.copy(valueStr = it.filter(Char::isDigit))
                }
                val quick = if (isAsset) listOf("10K" to 10000L, "1L" to 100000L, "5L" to 500000L, "10L" to 1000000L)
                else listOf("10K" to 10000L, "50K" to 50000L, "1L" to 100000L, "5L" to 500000L)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.forEach { (lbl, amt) ->
                        SelectChip(lbl, selected = false) {
                            val cur = draft.valueStr.toLongOrNull() ?: 0L
                            draft = draft.copy(valueStr = (cur + amt).toString())
                        }
                    }
                }
            }

            Label("Owner")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.members.forEach { m ->
                    SelectChip(m.name, selected = draft.owner == m.id, accent = hexToColor(m.color)) {
                        draft = draft.copy(owner = m.id)
                    }
                }
            }

            Field("Note (optional)", draft.note, "") { draft = draft.copy(note = it) }
            Field("Reference / link (optional)", draft.ref, "") { draft = draft.copy(ref = it) }

            Spacer(Modifier.height(4.dp))
            val accent = if (isAsset) nwColors.green else nwColors.red
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEdit) {
                    PillButton(
                        label = "Delete",
                        bg = tintFor(nwColors.red),
                        fg = nwColors.red,
                        onClick = { vm.deleteItem(kind, target.item!!.id); vm.closeEditor() },
                    )
                }
                Spacer(Modifier.weight(1f))
                PillButton(
                    label = "Cancel",
                    bg = nwColors.inputBg,
                    fg = nwColors.text2,
                    border = nwColors.chipBorder,
                    onClick = vm::closeEditor,
                )
                Spacer(Modifier.width(10.dp))
                PillButton(
                    label = if (isEdit) "Save changes" else if (isAsset) "Add asset" else "Add liability",
                    bg = if (canSave) accent else nwColors.inputBg,
                    fg = if (canSave) Color(0xFF0B0B0B) else nwColors.text3,
                    enabled = canSave,
                    onClick = {
                        val name = draft.name.trim().ifBlank {
                            if (byWeight) "$grams g ${draft.metal.key}" else cats.first { it.key == draft.cat }.label
                        }
                        vm.upsertItem(
                            kind,
                            Item(
                                id = target.item?.id ?: "",
                                name = name, cat = draft.cat, value = value, owner = draft.owner,
                                note = draft.note.trim().ifBlank { null },
                                ref = draft.ref.trim().ifBlank { null },
                                grams = if (byWeight) grams else null,
                                metal = if (byWeight) draft.metal else null,
                            ),
                        )
                        vm.closeEditor()
                    },
                )
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = nwColors.text3, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Field(
    label: String,
    value: String,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
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

@Composable
private fun PillButton(
    label: String,
    bg: Color,
    fg: Color,
    border: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(50),
        border = border?.let { BorderStroke(1.dp, it) },
        modifier = Modifier.height(48.dp),
    ) {
        Box(Modifier.fillMaxHeight().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
