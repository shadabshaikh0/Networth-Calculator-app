package com.shadabshaikh.networth.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A small, named type scale so sizes stay consistent instead of ad-hoc sp
 * values scattered across screens. Use `style = NwType.cardTitle`, etc.
 */
object NwType {
    val display = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold)
    val title = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold)
    val cardTitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
    val sectionLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val bodyStrong = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 14.sp)
    val caption = TextStyle(fontSize = 12.5.sp)
    val captionStrong = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
}
