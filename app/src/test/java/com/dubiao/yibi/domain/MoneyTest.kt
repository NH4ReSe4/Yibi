package com.dubiao.yibi.domain

import com.dubiao.yibi.data.CurrencyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun parsesCommaAndRoundsToCents() {
        assertEquals(1_235L, parseMinor("12,345"))
    }

    @Test fun rejectsInvalidAndOverflowingValues() {
        assertNull(parseMinor("十二"))
        assertNull(parseMinor("999999999999999999999999"))
    }

    @Test fun formatsNegativeAndGroupedEuroAmounts() {
        assertEquals("−€12,345.67", formatMoney(-1_234_567, CurrencyCode.EUR))
        assertEquals("+€12.00", formatMoney(1_200, CurrencyCode.EUR, signed = true))
    }
}
