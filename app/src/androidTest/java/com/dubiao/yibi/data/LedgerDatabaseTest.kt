package com.dubiao.yibi.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerDatabaseTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: LedgerDao

    @Before fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.ledgerDao()
    }

    @After fun closeDatabase() = database.close()

    @Test fun recurringPostingIsAtomicAndRejectsTheSameDueDateTwice() = runBlocking {
        val now = 1_777_777_777_000L
        val templateId = dao.saveRecurringTemplate(
            RecurringTemplateEntity(
                name = "房租", amountMinor = 80_000, expenseGroup = ExpenseGroup.FIXED,
                category = "房租", frequency = RecurrenceFrequency.MONTHLY, nextDueEpochDay = 21_500,
                note = "", createdAt = now, updatedAt = now,
            ),
        )
        val current = dao.observeRecurringTemplates().first().single()
        val transaction = TransactionEntity(
            type = TransactionType.EXPENSE, originalAmountMinor = 80_000, originalCurrency = CurrencyCode.EUR,
            eurAmountMinor = 80_000, exchangeRateMicros = 1_000_000, category = "房租",
            expenseGroup = ExpenseGroup.FIXED, occurredAt = now, note = "房租", inputMethod = InputMethod.MANUAL,
            rawVoiceText = null, createdAt = now, updatedAt = now,
        )
        val advanced = current.copy(nextDueEpochDay = 21_531)
        assertTrue(dao.postRecurringIfCurrent(transaction, advanced, expectedDueEpochDay = 21_500))
        assertTrue(!dao.postRecurringIfCurrent(transaction, advanced, expectedDueEpochDay = 21_500))
        assertEquals(1, dao.observeTransactions().first().size)
        assertEquals(templateId, dao.observeRecurringTemplates().first().single().id)
        assertEquals(21_531, dao.observeRecurringTemplates().first().single().nextDueEpochDay)
    }

    @Test fun replaceAllReplacesBothTables() = runBlocking {
        val now = 1_777_777_777_000L
        val transaction = TransactionEntity(
            id = 9, type = TransactionType.INCOME, originalAmountMinor = 100_000,
            originalCurrency = CurrencyCode.EUR, eurAmountMinor = 100_000, exchangeRateMicros = 1_000_000,
            category = "工资", expenseGroup = ExpenseGroup.DAILY, occurredAt = now, note = "工资",
            inputMethod = InputMethod.MANUAL, rawVoiceText = null, createdAt = now, updatedAt = now,
        )
        dao.replaceAll(listOf(transaction), emptyList())
        assertEquals(listOf(transaction), dao.observeTransactions().first())
        assertTrue(dao.observeRecurringTemplates().first().isEmpty())
    }

    private fun assertTrue(value: Boolean) = org.junit.Assert.assertTrue(value)
}
