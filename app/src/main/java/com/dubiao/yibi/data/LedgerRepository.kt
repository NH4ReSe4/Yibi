package com.dubiao.yibi.data

import kotlinx.coroutines.flow.Flow

class LedgerRepository(private val database: AppDatabase) {
    private val dao = database.ledgerDao()

    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val recurringTemplates: Flow<List<RecurringTemplateEntity>> = dao.observeRecurringTemplates()

    suspend fun saveTransaction(transaction: TransactionEntity) = dao.saveTransaction(transaction)
    suspend fun insertTransactions(transactions: List<TransactionEntity>) = dao.insertTransactions(transactions)
    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)
    suspend fun saveRecurringTemplate(template: RecurringTemplateEntity) = dao.saveRecurringTemplate(template)
    suspend fun deleteRecurringTemplate(template: RecurringTemplateEntity) = dao.deleteRecurringTemplate(template)
    suspend fun postRecurringIfCurrent(
        transaction: TransactionEntity,
        advancedTemplate: RecurringTemplateEntity,
        expectedDueEpochDay: Long,
    ) = dao.postRecurringIfCurrent(transaction, advancedTemplate, expectedDueEpochDay)
    suspend fun replaceAll(transactions: List<TransactionEntity>, templates: List<RecurringTemplateEntity>) =
        dao.replaceAll(transactions, templates)
    suspend fun clearTransactions() = dao.clearTransactions()
}
