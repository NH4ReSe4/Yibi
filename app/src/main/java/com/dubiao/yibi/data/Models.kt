package com.dubiao.yibi.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

enum class TransactionType { EXPENSE, INCOME }
enum class CurrencyCode { EUR }
enum class InputMethod { MANUAL, VOICE, RECURRING }
enum class ExpenseGroup { FIXED, SUBSCRIPTION, DAILY, INVESTMENT }
enum class RecurrenceFrequency { WEEKLY, MONTHLY, YEARLY }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val originalAmountMinor: Long,
    val originalCurrency: CurrencyCode,
    val eurAmountMinor: Long,
    val exchangeRateMicros: Long?,
    val category: String,
    @ColumnInfo(defaultValue = "'DAILY'") val expenseGroup: ExpenseGroup = ExpenseGroup.DAILY,
    val occurredAt: Long,
    val note: String,
    val inputMethod: InputMethod,
    val rawVoiceText: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "recurring_templates")
data class RecurringTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val expenseGroup: ExpenseGroup,
    val category: String,
    val frequency: RecurrenceFrequency,
    val nextDueEpochDay: Long,
    val note: String,
    val enabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CategoryTotal(
    val category: String,
    val totalMinor: Long,
)
