package com.shadabshaikh.networth

import com.shadabshaikh.networth.data.DEFAULT_MEMBERS
import com.shadabshaikh.networth.data.SEED_ASSETS
import com.shadabshaikh.networth.data.SEED_LIAB
import com.shadabshaikh.networth.domain.DeriveInput
import com.shadabshaikh.networth.domain.derive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeriveTest {

    private fun seedInput(included: Map<String, Boolean> = mapOf("self" to true, "spouse" to true, "parent" to true)) =
        DeriveInput(
            assets = SEED_ASSETS, liab = SEED_LIAB, members = DEFAULT_MEMBERS,
            included = included, rates = emptyMap(), history = emptyList(),
            currentMonth = "2026-08", onboardDismissed = false, catSel = null,
        )

    @Test
    fun seedTotals() {
        val d = derive(seedInput())
        assertEquals(13_800_000L, d.nw)
        assertEquals("₹1.38 Cr", d.nwCompact)
        assertEquals(19_545_000L, d.totalAssets)
        assertEquals(5_745_000L, d.totalLiab)
        assertEquals(12, d.assetCount)
        assertEquals(4, d.liabCount)
        assertFalse(d.isEmpty)
    }

    @Test
    fun emptyState() {
        val d = derive(seedInput().copy(assets = emptyList(), liab = emptyList()))
        assertEquals(0L, d.nw)
        assertTrue(d.isEmpty)
    }
}
