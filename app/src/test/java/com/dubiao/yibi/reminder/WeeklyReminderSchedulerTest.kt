package com.dubiao.yibi.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WeeklyReminderSchedulerTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun fridaySchedulesTheFollowingSaturdayAtTen() {
        val now = ZonedDateTime.of(2026, 8, 28, 18, 30, 0, 0, berlin)

        assertEquals(
            ZonedDateTime.of(2026, 8, 29, 10, 0, 0, 0, berlin),
            nextWeeklyReminder(now),
        )
    }

    @Test
    fun saturdayBeforeTenSchedulesTheSameDay() {
        val now = ZonedDateTime.of(2026, 8, 29, 9, 59, 0, 0, berlin)

        assertEquals(
            ZonedDateTime.of(2026, 8, 29, 10, 0, 0, 0, berlin),
            nextWeeklyReminder(now),
        )
    }

    @Test
    fun saturdayAtTenSchedulesTheNextWeek() {
        val now = ZonedDateTime.of(2026, 8, 29, 10, 0, 0, 0, berlin)

        assertEquals(
            ZonedDateTime.of(2026, 9, 5, 10, 0, 0, 0, berlin),
            nextWeeklyReminder(now),
        )
    }

    @Test
    fun sundaySchedulesTheNextSaturday() {
        val now = ZonedDateTime.of(2026, 8, 30, 11, 0, 0, 0, berlin)

        assertEquals(
            ZonedDateTime.of(2026, 9, 5, 10, 0, 0, 0, berlin),
            nextWeeklyReminder(now),
        )
    }
}
