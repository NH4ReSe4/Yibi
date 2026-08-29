package com.dubiao.yibi.domain

import com.dubiao.yibi.data.RecurrenceFrequency
import java.time.LocalDate

fun followingRecurringDate(
    dueDate: LocalDate,
    frequency: RecurrenceFrequency,
): LocalDate = when (frequency) {
    RecurrenceFrequency.WEEKLY -> dueDate.plusWeeks(1)
    RecurrenceFrequency.MONTHLY -> dueDate.plusMonths(1)
    RecurrenceFrequency.YEARLY -> dueDate.plusYears(1)
}

fun nextRecurringDate(
    dueDate: LocalDate,
    frequency: RecurrenceFrequency,
    today: LocalDate = LocalDate.now(),
): LocalDate {
    var next = dueDate
    do {
        next = followingRecurringDate(next, frequency)
    } while (!next.isAfter(today))
    return next
}
