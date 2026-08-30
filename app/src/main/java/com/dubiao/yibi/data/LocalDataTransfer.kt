package com.dubiao.yibi.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class BackupPayload(
    val transactions: List<TransactionEntity>,
    val recurringTemplates: List<RecurringTemplateEntity>,
    val billingCloseDay: Int,
    val budgetSettings: BudgetSettings,
    val weeklyReminderEnabled: Boolean,
)

object LocalDataTransfer {
    fun writeBackup(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>,
        templates: List<RecurringTemplateEntity>,
        billingCloseDay: Int,
        budgets: BudgetSettings,
        weeklyReminderEnabled: Boolean = true,
    ) {
        val root = JSONObject()
            .put("format", "yibi-backup")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("billingCloseDay", billingCloseDay)
            .put("weeklyReminderEnabled", weeklyReminderEnabled)
            .put("budgets", budgets.toJson())
            .put("transactions", JSONArray().apply { transactions.forEach { put(it.toJson()) } })
            .put("recurringTemplates", JSONArray().apply { templates.forEach { put(it.toJson()) } })
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "无法打开备份文件" }
            .bufferedWriter().use { it.write(root.toString(2)) }
    }

    fun readBackup(context: Context, uri: Uri): BackupPayload {
        val text = requireNotNull(context.contentResolver.openInputStream(uri)) { "无法读取备份文件" }
            .bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        require(root.optString("format") == "yibi-backup") { "不是有效的一笔备份文件" }
        require(root.optInt("version") == 1) { "暂不支持该备份版本" }
        val transactionArray = root.getJSONArray("transactions")
        val templateArray = root.optJSONArray("recurringTemplates") ?: JSONArray()
        return BackupPayload(
            transactions = List(transactionArray.length()) { transactionArray.getJSONObject(it).toTransaction() },
            recurringTemplates = List(templateArray.length()) { templateArray.getJSONObject(it).toTemplate() },
            billingCloseDay = root.optInt("billingCloseDay", 31).coerceIn(1, 31),
            budgetSettings = root.optJSONObject("budgets")?.toBudgets() ?: BudgetSettings(),
            weeklyReminderEnabled = root.optBoolean("weeklyReminderEnabled", true),
        )
    }

    fun writeCsv(context: Context, uri: Uri, transactions: List<TransactionEntity>) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "无法打开 CSV 文件" }
            .bufferedWriter().use { writer ->
            writer.append('\uFEFF')
            writer.appendLine("日期,类型,金额(EUR),开销大类,小类,备注,录入方式")
            transactions.sortedByDescending { it.occurredAt }.forEach { transaction ->
                val date = Instant.ofEpochMilli(transaction.occurredAt).atZone(ZoneId.systemDefault()).format(formatter)
                val amount = "%.2f".format(java.util.Locale.ROOT, transaction.eurAmountMinor / 100.0)
                writer.appendLine(
                    listOf(
                        date,
                        if (transaction.type == TransactionType.EXPENSE) "支出" else "收入",
                        amount,
                        if (transaction.type == TransactionType.EXPENSE) transaction.expenseGroup.csvLabel() else "",
                        transaction.category,
                        transaction.note,
                        when (transaction.inputMethod) {
                            InputMethod.VOICE -> "语音"
                            InputMethod.RECURRING -> "周期"
                            InputMethod.MANUAL -> "手动"
                        },
                    ).joinToString(",", transform = ::csvCell),
                )
            }
        }
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun ExpenseGroup.csvLabel(): String = when (this) {
        ExpenseGroup.FIXED -> "固定开销"
        ExpenseGroup.SUBSCRIPTION -> "订阅开销"
        ExpenseGroup.DAILY -> "日常消费"
        ExpenseGroup.INVESTMENT -> "投资花费"
    }

    private fun TransactionEntity.toJson() = JSONObject()
        .put("id", id).put("type", type.name).put("amountMinor", eurAmountMinor)
        .put("category", category).put("expenseGroup", expenseGroup.name)
        .put("occurredAt", occurredAt).put("note", note).put("inputMethod", inputMethod.name)
        .put("rawVoiceText", rawVoiceText ?: JSONObject.NULL)
        .put("createdAt", createdAt).put("updatedAt", updatedAt)

    private fun JSONObject.toTransaction(): TransactionEntity {
        val amount = getLong("amountMinor")
        require(amount > 0) { "备份中包含无效金额" }
        return TransactionEntity(
            id = optLong("id", 0), type = TransactionType.valueOf(getString("type")),
            originalAmountMinor = amount, originalCurrency = CurrencyCode.EUR, eurAmountMinor = amount,
            exchangeRateMicros = 1_000_000, category = getString("category"),
            expenseGroup = ExpenseGroup.valueOf(optString("expenseGroup", ExpenseGroup.DAILY.name)),
            occurredAt = getLong("occurredAt"), note = optString("note"),
            inputMethod = InputMethod.valueOf(optString("inputMethod", InputMethod.MANUAL.name)),
            rawVoiceText = if (isNull("rawVoiceText")) null else optString("rawVoiceText"),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            updatedAt = optLong("updatedAt", System.currentTimeMillis()),
        )
    }

    private fun RecurringTemplateEntity.toJson() = JSONObject()
        .put("id", id).put("name", name).put("amountMinor", amountMinor)
        .put("expenseGroup", expenseGroup.name).put("category", category)
        .put("frequency", frequency.name).put("nextDueEpochDay", nextDueEpochDay)
        .put("note", note).put("enabled", enabled)
        .put("createdAt", createdAt).put("updatedAt", updatedAt)

    private fun JSONObject.toTemplate(): RecurringTemplateEntity {
        val name = getString("name").trim()
        val amount = getLong("amountMinor")
        require(name.isNotBlank()) { "备份中包含无效的周期账目名称" }
        require(amount > 0) { "备份中包含无效的周期账目金额" }
        return RecurringTemplateEntity(
            id = optLong("id", 0), name = name, amountMinor = amount,
            expenseGroup = ExpenseGroup.valueOf(getString("expenseGroup")), category = getString("category"),
            frequency = RecurrenceFrequency.valueOf(getString("frequency")),
            nextDueEpochDay = getLong("nextDueEpochDay"), note = optString("note"),
            enabled = optBoolean("enabled", true), createdAt = optLong("createdAt", System.currentTimeMillis()),
            updatedAt = optLong("updatedAt", System.currentTimeMillis()),
        )
    }

    private fun BudgetSettings.toJson() = JSONObject()
        .put("totalMinor", totalMinor).put("fixedMinor", fixedMinor)
        .put("subscriptionMinor", subscriptionMinor).put("dailyMinor", dailyMinor)
        .put("investmentMinor", investmentMinor)

    private fun JSONObject.toBudgets(): BudgetSettings {
        val settings = BudgetSettings(
            totalMinor = optLong("totalMinor"), fixedMinor = optLong("fixedMinor"),
            subscriptionMinor = optLong("subscriptionMinor"), dailyMinor = optLong("dailyMinor"),
            investmentMinor = optLong("investmentMinor"),
        )
        require(
            listOf(
                settings.totalMinor, settings.fixedMinor, settings.subscriptionMinor,
                settings.dailyMinor, settings.investmentMinor,
            ).all { it >= 0 },
        ) { "备份中包含无效的预算金额" }
        return settings
    }
}
