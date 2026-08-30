package com.dubiao.yibi.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dubiao.yibi.data.CurrencyCode
import com.dubiao.yibi.data.BudgetSettings
import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.InputMethod
import com.dubiao.yibi.data.TransactionEntity
import com.dubiao.yibi.data.TransactionType
import com.dubiao.yibi.domain.RecurringBudgetReserve
import com.dubiao.yibi.ui.theme.YiBiTheme
import com.dubiao.yibi.update.AppUpdateState
import java.time.LocalDateTime
import java.time.ZoneId

private val previewNow = LocalDateTime.now().withSecond(0).withNano(0)

private fun previewTransaction(
    id: Long,
    daysAgo: Long,
    type: TransactionType,
    originalMinor: Long,
    currency: CurrencyCode,
    eurMinor: Long,
    category: String,
    note: String,
    expenseGroup: ExpenseGroup = ExpenseGroup.DAILY,
) = TransactionEntity(
    id = id,
    type = type,
    originalAmountMinor = originalMinor,
    originalCurrency = currency,
    eurAmountMinor = eurMinor,
    exchangeRateMicros = if (currency == CurrencyCode.EUR) 1_000_000 else null,
    category = category,
    expenseGroup = expenseGroup,
    occurredAt = previewNow.minusDays(daysAgo).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    note = note,
    inputMethod = InputMethod.MANUAL,
    rawVoiceText = null,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),
)

private val previewTransactions = listOf(
    previewTransaction(1, 0, TransactionType.EXPENSE, 420, CurrencyCode.EUR, 420, "餐饮", "晨间咖啡"),
    previewTransaction(2, 0, TransactionType.EXPENSE, 1280, CurrencyCode.EUR, 1280, "餐饮", "工作日午饭"),
    previewTransaction(3, 1, TransactionType.EXPENSE, 2460, CurrencyCode.EUR, 2460, "购物", "超市补给"),
    previewTransaction(4, 2, TransactionType.EXPENSE, 1713, CurrencyCode.EUR, 1713, "影音订阅", "线上订阅", ExpenseGroup.SUBSCRIPTION),
    previewTransaction(5, 3, TransactionType.EXPENSE, 1500, CurrencyCode.EUR, 1500, "餐饮", "和朋友吃饭"),
    previewTransaction(6, 4, TransactionType.EXPENSE, 490, CurrencyCode.EUR, 490, "交通", "地铁周票补差"),
    previewTransaction(7, 5, TransactionType.EXPENSE, 1450, CurrencyCode.EUR, 1450, "娱乐", "电影票"),
    previewTransaction(8, 7, TransactionType.INCOME, 320000, CurrencyCode.EUR, 320000, "收入", "本月工资"),
)

@Preview(name = "首页 · Pixel", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun HomePreview() {
    YiBiTheme(darkTheme = false) {
        HomeScreen(
            transactions = previewTransactions,
            billingCloseDay = 31,
            budgetSettings = BudgetSettings(totalMinor = 180000),
            isListening = false,
            onManual = {},
            onVoice = {},
            onSeeAll = {},
            onTransaction = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "报表 · Pixel", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun ReportsPreview() {
    YiBiTheme(darkTheme = false) {
        ReportsScreen(
            transactions = previewTransactions,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "记账确认 · Pixel", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun EditorPreview() {
    YiBiTheme(darkTheme = false) {
        TransactionEditorSheet(
            state = EditorState(
                visible = true,
                amountText = "83.5",
                category = "餐饮",
                note = "昨天午饭八十三块五",
                inputMethod = InputMethod.VOICE,
                rawVoiceText = "昨天午饭八十三块五",
            ),
            onDismiss = {},
            onType = {},
            onAmount = {},
            onCategory = {},
            onExpenseGroup = {},
            onDate = {},
            onNote = {},
            onSave = {},
            onDelete = {},
        )
    }
}

@Preview(name = "设置", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun SettingsPreview() {
    YiBiTheme(darkTheme = false) {
        SettingsScreen(
            billingCloseDay = 25,
            budgetSettings = BudgetSettings(totalMinor = 180000, dailyMinor = 60000),
            recurringReserve = RecurringBudgetReserve(),
            recurringTemplates = emptyList(),
            onBillingCloseDay = {},
            onEditBudget = {},
            onAddRecurring = {},
            onEditRecurring = {},
            onDeleteRecurring = {},
            weeklyReminderEnabled = true,
            notificationPermissionGranted = true,
            onWeeklyReminderEnabled = {},
            onRequestNotificationPermission = {},
            onExportBackup = {},
            onRestoreBackup = {},
            onExportCsv = {},
            updateState = AppUpdateState.Idle,
            onCheckUpdate = {},
            onDownloadUpdate = {},
            onInstallUpdate = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
