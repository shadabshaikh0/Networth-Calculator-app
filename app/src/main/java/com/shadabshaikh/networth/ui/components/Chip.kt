package com.shadabshaikh.networth.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadabshaikh.networth.ui.theme.nwColors
import com.shadabshaikh.networth.ui.theme.tintFor

/** A pill that shows selected/idle state, optionally tinted with an accent color. */
@Composable
fun SelectChip(label: String, selected: Boolean, accent: Color? = null, onClick: () -> Unit) {
    val color = accent ?: nwColors.gold
    Surface(
        color = if (selected) tintFor(color) else nwColors.bg,
        contentColor = if (selected) nwColors.text else nwColors.text2,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (selected) color else nwColors.chipBorder),
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
