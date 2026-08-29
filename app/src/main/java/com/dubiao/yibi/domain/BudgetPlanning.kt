package com.dubiao.yibi.domain

import com.dubiao.yibi.data.BudgetSettings
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

fun linkedBudgetSettings(
    totalMinor: Long,
    fixedMinor: Long,
    subscriptionMinor: Long,
    investmentRatio: Float,
): BudgetSettings {
    val total = totalMinor.coerceAtLeast(0)
    val fixed = fixedMinor.coerceAtLeast(0)
    val subscription = subscriptionMinor.coerceAtLeast(0)
    val flexible = (total - fixed - subscription).coerceAtLeast(0)
    val investment = (flexible * investmentRatio.coerceIn(0f, 1f)).roundToLong()
    return BudgetSettings(
        totalMinor = total,
        fixedMinor = fixed,
        subscriptionMinor = subscription,
        investmentMinor = investment,
        dailyMinor = flexible - investment,
    )
}

fun weeklyFlexibleAllowance(
    settings: BudgetSettings,
    cycleStart: LocalDate,
    cycleEndInclusive: LocalDate,
): Long {
    val cycleDays = ChronoUnit.DAYS.between(cycleStart, cycleEndInclusive).plus(1).coerceAtLeast(1)
    return (settings.dailyMinor.toDouble() * 7.0 / cycleDays).roundToLong()
}
