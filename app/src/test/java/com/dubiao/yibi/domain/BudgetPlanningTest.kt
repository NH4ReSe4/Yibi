package com.dubiao.yibi.domain

import com.dubiao.yibi.data.BudgetSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BudgetPlanningTest {
    @Test fun remainingBudgetIsSplitWithoutChangingTotal() {
        val settings = linkedBudgetSettings(
            totalMinor = 200_000,
            fixedMinor = 80_000,
            subscriptionMinor = 20_000,
            investmentRatio = .3f,
        )
        assertEquals(30_000, settings.investmentMinor)
        assertEquals(70_000, settings.dailyMinor)
        assertEquals(
            settings.totalMinor,
            settings.fixedMinor + settings.subscriptionMinor + settings.investmentMinor + settings.dailyMinor,
        )
    }

    @Test fun weeklyAllowanceUsesActualCycleLength() {
        assertEquals(
            22_581,
            weeklyFlexibleAllowance(
                BudgetSettings(dailyMinor = 100_000),
                LocalDate.of(2026, 8, 26),
                LocalDate.of(2026, 9, 25),
            ),
        )
    }

    @Test fun overReservedBudgetLeavesNoFlexibleAmount() {
        val settings = linkedBudgetSettings(100, 80, 40, .5f)
        assertEquals(0, settings.dailyMinor)
        assertEquals(0, settings.investmentMinor)
    }

    @Test fun ratioEndsAllocateAllFlexibleMoney() {
        assertEquals(10_000, linkedBudgetSettings(10_000, 0, 0, 0f).dailyMinor)
        assertEquals(10_000, linkedBudgetSettings(10_000, 0, 0, 1f).investmentMinor)
    }

    @Test fun roundingNeverLosesACent() {
        val settings = linkedBudgetSettings(10_001, 1_000, 2_000, .35f)
        assertEquals(
            settings.totalMinor,
            settings.fixedMinor + settings.subscriptionMinor + settings.investmentMinor + settings.dailyMinor,
        )
    }
}
