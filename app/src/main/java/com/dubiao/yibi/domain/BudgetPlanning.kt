package com.dubiao.yibi.domain

import com.dubiao.yibi.data.BudgetSettings
import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.RecurrenceFrequency
import com.dubiao.yibi.data.RecurringTemplateEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

data class RecurringBudgetReserve(
    val fixedMinor: Long = 0,
    val subscriptionMinor: Long = 0,
    val linkedTemplateCount: Int = 0,
)

fun recurringBudgetReserve(templates: List<RecurringTemplateEntity>): RecurringBudgetReserve {
    val linked = templates.filter {
        it.enabled && (it.expenseGroup == ExpenseGroup.FIXED || it.expenseGroup == ExpenseGroup.SUBSCRIPTION)
    }
    fun monthlyAmount(group: ExpenseGroup): Long {
        val grouped = linked.filter { it.expenseGroup == group }.groupBy { it.frequency }
        val weekly = grouped[RecurrenceFrequency.WEEKLY].orEmpty().sumOf { it.amountMinor }
        val monthly = grouped[RecurrenceFrequency.MONTHLY].orEmpty().sumOf { it.amountMinor }
        val yearly = grouped[RecurrenceFrequency.YEARLY].orEmpty().sumOf { it.amountMinor }
        return monthly + (weekly * 52.0 / 12.0).roundToLong() + (yearly / 12.0).roundToLong()
    }
    return RecurringBudgetReserve(
        fixedMinor = monthlyAmount(ExpenseGroup.FIXED),
        subscriptionMinor = monthlyAmount(ExpenseGroup.SUBSCRIPTION),
        linkedTemplateCount = linked.size,
    )
}

fun budgetSettingsWithRecurringReserve(
    settings: BudgetSettings,
    reserve: RecurringBudgetReserve,
): BudgetSettings {
    if (settings.totalMinor <= 0) return settings
    val fixed = maxOf(settings.fixedMinor, reserve.fixedMinor)
    val subscription = maxOf(settings.subscriptionMinor, reserve.subscriptionMinor)
    if (fixed == settings.fixedMinor && subscription == settings.subscriptionMinor) return settings
    val flexible = settings.investmentMinor + settings.dailyMinor
    val investmentRatio = if (flexible > 0) settings.investmentMinor.toFloat() / flexible else .3f
    return linkedBudgetSettings(
        totalMinor = settings.totalMinor,
        fixedMinor = fixed,
        subscriptionMinor = subscription,
        investmentRatio = investmentRatio,
    )
}

fun relinkBudgetSettings(
    settings: BudgetSettings,
    previousReserve: RecurringBudgetReserve,
    currentReserve: RecurringBudgetReserve,
): BudgetSettings {
    if (settings.totalMinor <= 0) return settings
    if (
        previousReserve.fixedMinor == currentReserve.fixedMinor &&
        previousReserve.subscriptionMinor == currentReserve.subscriptionMinor
    ) return settings
    val flexible = settings.investmentMinor + settings.dailyMinor
    val investmentRatio = if (flexible > 0) settings.investmentMinor.toFloat() / flexible else .3f
    val extraFixed = (settings.fixedMinor - previousReserve.fixedMinor).coerceAtLeast(0)
    val extraSubscription = (settings.subscriptionMinor - previousReserve.subscriptionMinor).coerceAtLeast(0)
    return linkedBudgetSettings(
        totalMinor = settings.totalMinor,
        fixedMinor = currentReserve.fixedMinor + extraFixed,
        subscriptionMinor = currentReserve.subscriptionMinor + extraSubscription,
        investmentRatio = investmentRatio,
    )
}

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
