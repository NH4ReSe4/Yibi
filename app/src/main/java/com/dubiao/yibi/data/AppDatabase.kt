package com.dubiao.yibi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabaseConverters {
    @TypeConverter fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    @TypeConverter fun fromCurrency(value: CurrencyCode): String = value.name
    @TypeConverter fun toCurrency(value: String): CurrencyCode = CurrencyCode.valueOf(value)
    @TypeConverter fun fromInputMethod(value: InputMethod): String = value.name
    @TypeConverter fun toInputMethod(value: String): InputMethod = InputMethod.valueOf(value)
    @TypeConverter fun fromExpenseGroup(value: ExpenseGroup): String = value.name
    @TypeConverter fun toExpenseGroup(value: String): ExpenseGroup = ExpenseGroup.valueOf(value)
    @TypeConverter fun fromRecurrenceFrequency(value: RecurrenceFrequency): String = value.name
    @TypeConverter fun toRecurrenceFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)
}

@Database(
    entities = [TransactionEntity::class, RecurringTemplateEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN expenseGroup TEXT NOT NULL DEFAULT 'DAILY'")
                db.execSQL(
                    """UPDATE transactions SET expenseGroup = 'FIXED'
                        WHERE category = '居住'
                        OR note LIKE '%房租%' OR note LIKE '%水费%' OR note LIKE '%电费%'
                        OR note LIKE '%燃气%' OR note LIKE '%物业%' OR note LIKE '%保险%'""".trimIndent(),
                )
                db.execSQL(
                    """UPDATE transactions SET expenseGroup = 'SUBSCRIPTION'
                        WHERE note LIKE '%订阅%' OR note LIKE '%会员%' OR note LIKE '%月费%'
                        OR note LIKE '%Netflix%' OR note LIKE '%Spotify%'""".trimIndent(),
                )
                db.execSQL(
                    """UPDATE transactions SET expenseGroup = 'INVESTMENT'
                        WHERE note LIKE '%投资%' OR note LIKE '%股票%' OR note LIKE '%基金%'
                        OR note LIKE '%定投%' OR note LIKE '%ETF定投%'
                        OR note = 'ETF' OR note LIKE 'ETF %' OR note LIKE '% ETF' OR note LIKE '% ETF %'""".trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """UPDATE transactions
                        SET originalAmountMinor = eurAmountMinor,
                            originalCurrency = 'EUR',
                            exchangeRateMicros = 1000000""".trimIndent(),
                )
                db.execSQL("DROP TABLE currency_rates")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS recurring_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        amountMinor INTEGER NOT NULL,
                        expenseGroup TEXT NOT NULL,
                        category TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        nextDueEpochDay INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )""".trimIndent(),
                )
            }
        }

        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "yibi.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }
}
