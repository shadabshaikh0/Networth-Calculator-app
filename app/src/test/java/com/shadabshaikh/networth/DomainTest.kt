package com.shadabshaikh.networth

import com.shadabshaikh.networth.data.DEFAULT_RATES
import com.shadabshaikh.networth.data.SEED_ASSETS
import com.shadabshaikh.networth.data.SEED_LIAB
import com.shadabshaikh.networth.domain.compact
import com.shadabshaikh.networth.domain.inr
import com.shadabshaikh.networth.domain.monthLabel
import com.shadabshaikh.networth.domain.netWorthOf
import com.shadabshaikh.networth.domain.resolveValue
import com.shadabshaikh.networth.model.Item
import com.shadabshaikh.networth.model.Metal
import org.junit.Assert.assertEquals
import org.junit.Test

class DomainTest {

    private val allIncluded = mapOf("self" to true, "spouse" to true, "parent" to true)

    @Test
    fun seedNetWorth_isKnownTotal() {
        // assets ₹1,95,45,000 − liabilities ₹57,45,000 = ₹1,38,00,000
        val nw = netWorthOf(SEED_ASSETS, SEED_LIAB, allIncluded, emptyMap())
        assertEquals(13_800_000L, nw)
    }

    @Test
    fun excludingAnOwner_dropsTheirItemsFromTotals() {
        val excludeSpouse = allIncluded + ("spouse" to false)
        // spouse assets a7+a9+a10 = 1,540,000 ; spouse liab l4 = 180,000
        val nw = netWorthOf(SEED_ASSETS, SEED_LIAB, excludeSpouse, emptyMap())
        assertEquals(13_800_000L - 1_540_000L + 180_000L, nw)
    }

    @Test
    fun hiddenItem_excludedFromTotals() {
        val assets = SEED_ASSETS.map { if (it.id == "a8") it.copy(hidden = true) else it }
        val nw = netWorthOf(assets, SEED_LIAB, allIncluded, emptyMap())
        assertEquals(13_800_000L - 8_500_000L, nw) // 2BHK Pune hidden
    }

    @Test
    fun absentOwnerInMap_countsAsIncluded() {
        // JS parity: included[owner] !== false — a missing key is still included.
        val nw = netWorthOf(SEED_ASSETS, SEED_LIAB, emptyMap(), emptyMap())
        assertEquals(13_800_000L, nw)
    }

    @Test
    fun goldByWeight_pricedFromRate() {
        val item = Item(id = "g", name = "Coins", cat = "gold", value = 0, grams = 10.0, metal = Metal.GOLD)
        assertEquals(72_500L, resolveValue(item, DEFAULT_RATES)) // 10 × 7250
    }

    @Test
    fun noMetal_usesStoredValue() {
        val item = Item(id = "x", name = "Cash", cat = "cash", value = 145_000L)
        assertEquals(145_000L, resolveValue(item, DEFAULT_RATES))
    }

    @Test
    fun passedRates_overrideDefaults() {
        val item = Item(id = "g", name = "Coins", cat = "gold", value = 0, grams = 10.0, metal = Metal.GOLD)
        val nw = netWorthOf(listOf(item), emptyList(), allIncluded, mapOf("gold" to 8000L))
        assertEquals(80_000L, nw) // 10 × 8000
    }

    @Test
    fun compact_crAndLakhBoundaries() {
        assertEquals("₹1.38 Cr", compact(13_800_000L))
        assertEquals("₹1.23 Cr", compact(12_345_678L))
        assertEquals("₹1 L", compact(100_000L))
        assertEquals("₹10 L", compact(999_999L)) // 9.99999 rounds to 10.00 → "10"
        assertEquals("₹99,999", compact(99_999L))
        assertEquals("-₹1.38 Cr", compact(-13_800_000L))
    }

    @Test
    fun inr_usesIndianGrouping() {
        assertEquals("₹1,23,45,678", inr(12_345_678L))
        assertEquals("₹1,45,000", inr(145_000L))
    }

    @Test
    fun monthLabel_shortAndWithYear() {
        assertEquals("Jul", monthLabel("2026-07"))
        assertEquals("Jul '26", monthLabel("2026-07", withYear = true))
        assertEquals("Jan", monthLabel("2026-01"))
    }
}
