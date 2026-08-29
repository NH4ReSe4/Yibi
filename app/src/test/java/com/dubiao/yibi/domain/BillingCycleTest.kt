package com.dubiao.yibi.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BillingCycleTest {
    @Test
    fun `25日之前属于上月26日至本月25日`() {
        val cycle = billingCycleFor(LocalDate.of(2026, 8, 20), 25)

        assertEquals(LocalDate.of(2026, 7, 26), cycle.start)
        assertEquals(LocalDate.of(2026, 8, 25), cycle.endInclusive)
    }

    @Test
    fun `25日之后进入当月26日至下月25日`() {
        val cycle = billingCycleFor(LocalDate.of(2026, 8, 26), 25)

        assertEquals(LocalDate.of(2026, 8, 26), cycle.start)
        assertEquals(LocalDate.of(2026, 9, 25), cycle.endInclusive)
    }

    @Test
    fun `31日截止在短月份自动使用月末`() {
        val cycle = billingCycleFor(LocalDate.of(2026, 2, 20), 31)

        assertEquals(LocalDate.of(2026, 2, 1), cycle.start)
        assertEquals(LocalDate.of(2026, 2, 28), cycle.endInclusive)
    }

    @Test fun `闰年二月使用29日`() {
        val cycle = billingCycleFor(LocalDate.of(2028, 2, 20), 31)
        assertEquals(LocalDate.of(2028, 2, 1), cycle.start)
        assertEquals(LocalDate.of(2028, 2, 29), cycle.endInclusive)
    }

    @Test fun `每月1日截止正确跨月`() {
        val cycle = billingCycleFor(LocalDate.of(2026, 8, 2), 1)
        assertEquals(LocalDate.of(2026, 8, 2), cycle.start)
        assertEquals(LocalDate.of(2026, 9, 1), cycle.endInclusive)
    }
}
