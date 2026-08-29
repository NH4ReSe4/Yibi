package com.dubiao.yibi.domain

import java.time.LocalDate
import java.time.YearMonth

data class BillingCycle(
    val start: LocalDate,
    val endInclusive: LocalDate,
)

fun billingCycleFor(date: LocalDate, closeDay: Int): BillingCycle {
    val normalizedDay = closeDay.coerceIn(1, 31)
    val month = YearMonth.from(date)
    val closeInCurrentMonth = closeDate(month, normalizedDay)

    return if (date <= closeInCurrentMonth) {
        BillingCycle(
            start = closeDate(month.minusMonths(1), normalizedDay).plusDays(1),
            endInclusive = closeInCurrentMonth,
        )
    } else {
        BillingCycle(
            start = closeInCurrentMonth.plusDays(1),
            endInclusive = closeDate(month.plusMonths(1), normalizedDay),
        )
    }
}

private fun closeDate(month: YearMonth, closeDay: Int): LocalDate =
    month.atDay(closeDay.coerceAtMost(month.lengthOfMonth()))
