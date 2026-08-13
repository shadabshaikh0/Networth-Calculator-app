package com.shadabshaikh.networth

import com.shadabshaikh.networth.domain.recordSnapshot
import com.shadabshaikh.networth.model.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HistoryTest {

    @Test
    fun insertsNewMonth_sorted() {
        val h = listOf(Snapshot("2026-06", 100), Snapshot("2026-08", 300))
        val out = recordSnapshot(h, "2026-07", 200)
        assertEquals(listOf("2026-06", "2026-07", "2026-08"), out.map { it.month })
    }

    @Test
    fun updatesExistingMonth() {
        val h = listOf(Snapshot("2026-08", 300))
        val out = recordSnapshot(h, "2026-08", 555)
        assertEquals(listOf(Snapshot("2026-08", 555)), out)
    }

    @Test
    fun sameValue_returnsSameListUnchanged() {
        val h = listOf(Snapshot("2026-08", 300))
        val out = recordSnapshot(h, "2026-08", 300)
        assertSame(h, out) // identity: no change means no new list
    }
}
