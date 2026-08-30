package com.dubiao.yibi.domain

import com.dubiao.yibi.data.BudgetSettings
import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.RecurrenceFrequency
import com.dubiao.yibi.data.RecurringTemplateEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BudgetPlanningTest {
    private fun template(
        amountMinor: Long,
        group: ExpenseGroup,
        frequency: RecurrenceFrequency,
        enabled: Boolean = true,
    ) = RecurringTemplateEntity(
        name = "test",
        amountMinor = amountMinor,
        expenseGroup = group,
        category = "test",
        frequency = frequency,
        nextDueEpochDay = 0,
        note = "",
        enabled = enabled,
        createdAt = 0,
        updatedAt = 0,
    )

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

    @Test fun recurringFixedAndSubscriptionsAreConvertedToMonthlyReserve() {
        val reserve = recurringBudgetReserve(
            listOf(
                template(80_000, ExpenseGroup.FIXED, RecurrenceFrequency.MONTHLY),
                template(120_000, ExpenseGroup.FIXED, RecurrenceFrequency.YEARLY),
                template(500, ExpenseGroup.SUBSCRIPTION, RecurrenceFrequency.WEEKLY),
                template(999, ExpenseGroup.DAILY, RecurrenceFrequency.MONTHLY),
                template(999, ExpenseGroup.FIXED, RecurrenceFrequency.MONTHLY, enabled = false),
            ),
        )
        assertEquals(90_000, reserve.fixedMinor)
        assertEquals(2_167, reserve.subscriptionMinor)
        assertEquals(3, reserve.linkedTemplateCount)
    }

    @Test fun changingRecurringReservePreservesManualBufferAndFlexibleRatio() {
        val settings = BudgetSettings(
            totalMinor = 200_000,
            fixedMinor = 90_000,
            subscriptionMinor = 10_000,
            investmentMinor = 30_000,
            dailyMinor = 70_000,
        )
        val linked = relinkBudgetSettings(
            settings = settings,
            previousReserve = RecurringBudgetReserve(fixedMinor = 80_000, subscriptionMinor = 10_000),
            currentReserve = RecurringBudgetReserve(fixedMinor = 100_000, subscriptionMinor = 5_000),
        )
        assertEquals(110_000, linked.fixedMinor)
        assertEquals(5_000, linked.subscriptionMinor)
        assertEquals(25_500, linked.investmentMinor)
        assertEquals(59_500, linked.dailyMinor)
    }

    @Test fun initialLinkUsesRecurringAmountsAsMinimumReserve() {
        val linked = budgetSettingsWithRecurringReserve(
            BudgetSettings(totalMinor = 100_000, fixedMinor = 20_000, dailyMinor = 80_000),
            RecurringBudgetReserve(fixedMinor = 30_000),
        )
        assertEquals(30_000, linked.fixedMinor)
        assertEquals(70_000, linked.dailyMinor + linked.investmentMinor)
    }
}
