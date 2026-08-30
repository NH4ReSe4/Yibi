package com.dubiao.yibi.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BudgetSettings(
    val totalMinor: Long = 0,
    val fixedMinor: Long = 0,
    val subscriptionMinor: Long = 0,
    val dailyMinor: Long = 0,
    val investmentMinor: Long = 0,
) {
    fun forGroup(group: ExpenseGroup): Long = when (group) {
        ExpenseGroup.FIXED -> fixedMinor
        ExpenseGroup.SUBSCRIPTION -> subscriptionMinor
        ExpenseGroup.DAILY -> dailyMinor
        ExpenseGroup.INVESTMENT -> investmentMinor
    }
}

class UserPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val _billingCloseDay = MutableStateFlow(
        preferences.getInt(KEY_BILLING_CLOSE_DAY, DEFAULT_BILLING_CLOSE_DAY).coerceIn(1, 31),
    )

    val billingCloseDay = _billingCloseDay.asStateFlow()
    private val _budgetSettings = MutableStateFlow(readBudgetSettings())
    val budgetSettings = _budgetSettings.asStateFlow()
    private val _weeklyReminderEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_WEEKLY_REMINDER_ENABLED, true),
    )
    val weeklyReminderEnabled = _weeklyReminderEnabled.asStateFlow()

    fun setBillingCloseDay(day: Int) {
        val normalized = day.coerceIn(1, 31)
        preferences.edit().putInt(KEY_BILLING_CLOSE_DAY, normalized).apply()
        _billingCloseDay.value = normalized
    }

    fun setBudgetSettings(settings: BudgetSettings) {
        val normalized = settings.copy(
            totalMinor = settings.totalMinor.coerceAtLeast(0),
            fixedMinor = settings.fixedMinor.coerceAtLeast(0),
            subscriptionMinor = settings.subscriptionMinor.coerceAtLeast(0),
            dailyMinor = settings.dailyMinor.coerceAtLeast(0),
            investmentMinor = settings.investmentMinor.coerceAtLeast(0),
        )
        preferences.edit()
            .putLong(KEY_BUDGET_TOTAL, normalized.totalMinor)
            .putLong(KEY_BUDGET_FIXED, normalized.fixedMinor)
            .putLong(KEY_BUDGET_SUBSCRIPTION, normalized.subscriptionMinor)
            .putLong(KEY_BUDGET_DAILY, normalized.dailyMinor)
            .putLong(KEY_BUDGET_INVESTMENT, normalized.investmentMinor)
            .apply()
        _budgetSettings.value = normalized
    }

    fun setWeeklyReminderEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_WEEKLY_REMINDER_ENABLED, enabled).apply()
        _weeklyReminderEnabled.value = enabled
    }

    private fun readBudgetSettings() = BudgetSettings(
        totalMinor = preferences.getLong(KEY_BUDGET_TOTAL, 0),
        fixedMinor = preferences.getLong(KEY_BUDGET_FIXED, 0),
        subscriptionMinor = preferences.getLong(KEY_BUDGET_SUBSCRIPTION, 0),
        dailyMinor = preferences.getLong(KEY_BUDGET_DAILY, 0),
        investmentMinor = preferences.getLong(KEY_BUDGET_INVESTMENT, 0),
    )

    private companion object {
        const val FILE_NAME = "yibi_preferences"
        const val KEY_BILLING_CLOSE_DAY = "billing_close_day"
        const val KEY_BUDGET_TOTAL = "budget_total"
        const val KEY_BUDGET_FIXED = "budget_fixed"
        const val KEY_BUDGET_SUBSCRIPTION = "budget_subscription"
        const val KEY_BUDGET_DAILY = "budget_daily"
        const val KEY_BUDGET_INVESTMENT = "budget_investment"
        const val KEY_WEEKLY_REMINDER_ENABLED = "weekly_reminder_enabled"
        const val DEFAULT_BILLING_CLOSE_DAY = 31
    }
}
