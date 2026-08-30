package com.dubiao.yibi.data

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalDataTransferTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun backupRoundTripPreservesEverySupportedField() {
        val now = 1_777_777_777_000L
        val transaction = TransactionEntity(
            id = 7, type = TransactionType.EXPENSE, originalAmountMinor = 1_299,
            originalCurrency = CurrencyCode.EUR, eurAmountMinor = 1_299, exchangeRateMicros = 1_000_000,
            category = "餐饮", expenseGroup = ExpenseGroup.DAILY, occurredAt = now,
            note = "咖啡，\"双份\"", inputMethod = InputMethod.VOICE, rawVoiceText = "咖啡十二点九九欧",
            createdAt = now, updatedAt = now,
        )
        val template = RecurringTemplateEntity(
            id = 3, name = "测试订阅", amountMinor = 999, expenseGroup = ExpenseGroup.SUBSCRIPTION,
            category = "软件服务", frequency = RecurrenceFrequency.MONTHLY, nextDueEpochDay = 21_500,
            note = "每月", createdAt = now, updatedAt = now,
        )
        val budgets = BudgetSettings(200_000, 80_000, 20_000, 70_000, 30_000)
        val file = File(context.cacheDir, "roundtrip-${System.nanoTime()}.json")
        try {
            LocalDataTransfer.writeBackup(context, Uri.fromFile(file), listOf(transaction), listOf(template), 25, budgets, false)
            val restored = LocalDataTransfer.readBackup(context, Uri.fromFile(file))
            assertEquals(listOf(transaction), restored.transactions)
            assertEquals(listOf(template), restored.recurringTemplates)
            assertEquals(25, restored.billingCloseDay)
            assertEquals(budgets, restored.budgetSettings)
            assertFalse(restored.weeklyReminderEnabled)
        } finally {
            file.delete()
        }
    }

    @Test fun csvUsesBomChineseLabelsAndEscapesQuotes() {
        val now = 1_777_777_777_000L
        val transaction = TransactionEntity(
            type = TransactionType.EXPENSE, originalAmountMinor = 1_250, originalCurrency = CurrencyCode.EUR,
            eurAmountMinor = 1_250, exchangeRateMicros = 1_000_000, category = "软件服务",
            expenseGroup = ExpenseGroup.SUBSCRIPTION, occurredAt = now, note = "A, \"B\"",
            inputMethod = InputMethod.MANUAL, rawVoiceText = null, createdAt = now, updatedAt = now,
        )
        val file = File(context.cacheDir, "csv-${System.nanoTime()}.csv")
        try {
            LocalDataTransfer.writeCsv(context, Uri.fromFile(file), listOf(transaction))
            val text = file.readText()
            assertTrue(text.startsWith("\uFEFF日期,类型,金额(EUR)"))
            assertTrue(text.contains("\"订阅开销\""))
            assertTrue(text.contains("\"A, \"\"B\"\"\""))
        } finally {
            file.delete()
        }
    }
}
