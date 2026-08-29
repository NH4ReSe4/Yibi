package com.dubiao.yibi.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dubiao.yibi.data.AppDatabase
import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.InputMethod
import com.dubiao.yibi.data.LedgerRepository
import com.dubiao.yibi.data.RecurrenceFrequency
import com.dubiao.yibi.data.RecurringTemplateEntity
import com.dubiao.yibi.data.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RecurringAutoSyncTest {
    @Test fun overdueTemplatePostsAutomaticallyUsingItsDueDate() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = LedgerRepository(database)
            LedgerViewModel(repository, UserPreferences(context))
            val dueDate = LocalDate.now().minusDays(3)
            val now = System.currentTimeMillis()

            repository.saveRecurringTemplate(
                RecurringTemplateEntity(
                    name = "保险",
                    amountMinor = 4_500,
                    expenseGroup = ExpenseGroup.FIXED,
                    category = "保险",
                    frequency = RecurrenceFrequency.MONTHLY,
                    nextDueEpochDay = dueDate.toEpochDay(),
                    note = "家庭保险",
                    createdAt = now,
                    updatedAt = now,
                ),
            )

            val transaction = withTimeout(5_000) {
                repository.transactions.first { it.isNotEmpty() }.single()
            }
            val postedDate = Instant.ofEpochMilli(transaction.occurredAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            assertEquals(dueDate, postedDate)
            assertEquals(InputMethod.RECURRING, transaction.inputMethod)

            val advancedTemplate = withTimeout(5_000) {
                repository.recurringTemplates.first { templates ->
                    templates.singleOrNull()?.nextDueEpochDay != dueDate.toEpochDay()
                }.single()
            }
            assertTrue(LocalDate.ofEpochDay(advancedTemplate.nextDueEpochDay).isAfter(dueDate))
        } finally {
            database.close()
        }
    }
}
