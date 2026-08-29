package com.dubiao.yibi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM recurring_templates ORDER BY nextDueEpochDay, id")
    fun observeRecurringTemplates(): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates WHERE id = :id LIMIT 1")
    suspend fun getRecurringTemplate(id: Long): RecurringTemplateEntity?

    @Upsert
    suspend fun saveTransaction(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Upsert
    suspend fun saveRecurringTemplate(template: RecurringTemplateEntity): Long

    @Delete
    suspend fun deleteRecurringTemplate(template: RecurringTemplateEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM recurring_templates")
    suspend fun clearRecurringTemplates()

    @Transaction
    suspend fun replaceAll(
        transactions: List<TransactionEntity>,
        templates: List<RecurringTemplateEntity>,
    ) {
        clearTransactions()
        clearRecurringTemplates()
        if (transactions.isNotEmpty()) insertTransactions(transactions)
        templates.forEach { saveRecurringTemplate(it) }
    }

    @Transaction
    suspend fun postRecurringIfCurrent(
        transaction: TransactionEntity,
        advancedTemplate: RecurringTemplateEntity,
        expectedDueEpochDay: Long,
    ): Boolean {
        val current = getRecurringTemplate(advancedTemplate.id)
        if (current == null || !current.enabled || current.nextDueEpochDay != expectedDueEpochDay) {
            return false
        }
        saveTransaction(transaction)
        saveRecurringTemplate(advancedTemplate)
        return true
    }
}
