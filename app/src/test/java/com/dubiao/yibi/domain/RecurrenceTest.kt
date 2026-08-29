package com.dubiao.yibi.domain

import com.dubiao.yibi.data.RecurrenceFrequency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {
    @Test fun followingDateAdvancesExactlyOnePeriod() {
        assertEquals(
            LocalDate.of(2026, 9, 26),
            followingRecurringDate(LocalDate.of(2026, 8, 26), RecurrenceFrequency.MONTHLY),
        )
    }

    @Test fun monthlyDateAdvancesBeyondToday() {
        assertEquals(
            LocalDate.of(2026, 9, 25),
            nextRecurringDate(
                dueDate = LocalDate.of(2026, 7, 25),
                frequency = RecurrenceFrequency.MONTHLY,
                today = LocalDate.of(2026, 8, 28),
            ),
        )
    }

    @Test fun monthEndRemainsValid() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            nextRecurringDate(
                dueDate = LocalDate.of(2026, 1, 31),
                frequency = RecurrenceFrequency.MONTHLY,
                today = LocalDate.of(2026, 1, 31),
            ),
        )
    }

    @Test fun weeklyOverdueAdvancesPastToday() {
        assertEquals(
            LocalDate.of(2026, 9, 4),
            nextRecurringDate(LocalDate.of(2026, 8, 7), RecurrenceFrequency.WEEKLY, LocalDate.of(2026, 8, 28)),
        )
    }

    @Test fun leapDayYearlyDateRemainsValid() {
        assertEquals(
            LocalDate.of(2027, 2, 28),
            nextRecurringDate(LocalDate.of(2024, 2, 29), RecurrenceFrequency.YEARLY, LocalDate.of(2026, 8, 28)),
        )
    }
}
