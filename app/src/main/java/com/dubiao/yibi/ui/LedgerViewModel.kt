package com.dubiao.yibi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import android.util.Log
import com.dubiao.yibi.BuildConfig
import com.dubiao.yibi.data.BudgetSettings
import com.dubiao.yibi.data.CurrencyCode
import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.InputMethod
import com.dubiao.yibi.data.LedgerRepository
import com.dubiao.yibi.data.LocalDataTransfer
import com.dubiao.yibi.data.RecurrenceFrequency
import com.dubiao.yibi.data.RecurringTemplateEntity
import com.dubiao.yibi.data.TransactionEntity
import com.dubiao.yibi.data.TransactionType
import com.dubiao.yibi.data.UserPreferences
import com.dubiao.yibi.domain.VoiceParser
import com.dubiao.yibi.domain.followingRecurringDate
import com.dubiao.yibi.domain.minorToInput
import com.dubiao.yibi.domain.parseMinor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId

data class EditorState(
    val visible: Boolean = false,
    val editingId: Long = 0,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val category: String = "餐饮",
    val expenseGroup: ExpenseGroup = ExpenseGroup.DAILY,
    val occurredAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val inputMethod: InputMethod = InputMethod.MANUAL,
    val rawVoiceText: String? = null,
    val error: String? = null,
)

data class RecurringEditorState(
    val visible: Boolean = false,
    val editingId: Long = 0,
    val name: String = "",
    val amountText: String = "",
    val expenseGroup: ExpenseGroup = ExpenseGroup.FIXED,
    val category: String = "房租",
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val nextDueDate: LocalDate = LocalDate.now(),
    val note: String = "",
    val error: String? = null,
)

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {
    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val billingCloseDay = userPreferences.billingCloseDay
    val budgetSettings = userPreferences.budgetSettings
    val recurringTemplates: StateFlow<List<RecurringTemplateEntity>> = repository.recurringTemplates.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _editor = MutableStateFlow(EditorState())
    val editor = _editor.asStateFlow()
    private val _recurringEditor = MutableStateFlow(RecurringEditorState())
    val recurringEditor = _recurringEditor.asStateFlow()
    private var editorSaveInFlight = false
    private var recurringSaveInFlight = false

    init {
        viewModelScope.launch {
            repository.recurringTemplates.collect { templates ->
                try {
                    synchronizeDueRecurringTemplates(templates)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    debugLog("recurring.sync.error", error.message ?: error.javaClass.simpleName)
                }
            }
        }
    }

    fun openManual() {
        _editor.value = EditorState(visible = true)
    }

    fun openVoiceResult(text: String) {
        val draft = VoiceParser.parse(text)
        debugLog("voice.parse", "text=$text, amount=${draft.amountText}, category=${draft.category}")
        _editor.value = EditorState(
            visible = true,
            type = draft.type,
            amountText = draft.amountText.orEmpty(),
            category = draft.category,
            expenseGroup = draft.expenseGroup,
            occurredAt = draft.occurredAt,
            note = draft.note,
            inputMethod = InputMethod.VOICE,
            rawVoiceText = draft.rawText,
        )
    }

    fun edit(transaction: TransactionEntity) {
        _editor.value = EditorState(
            visible = true,
            editingId = transaction.id,
            type = transaction.type,
            amountText = minorToInput(transaction.eurAmountMinor),
            category = when {
                transaction.type == TransactionType.INCOME && isIncomeCategory(transaction.category) -> transaction.category
                transaction.type == TransactionType.INCOME -> "其他收入"
                categoryBelongsTo(transaction.expenseGroup, transaction.category) -> transaction.category
                else -> defaultExpenseCategory(transaction.expenseGroup)
            },
            expenseGroup = transaction.expenseGroup,
            occurredAt = transaction.occurredAt,
            note = transaction.note,
            inputMethod = transaction.inputMethod,
            rawVoiceText = transaction.rawVoiceText,
        )
    }

    fun dismissEditor() {
        _editor.value = EditorState()
    }

    fun setType(value: TransactionType) = update {
        copy(
            type = value,
            category = when {
                value == TransactionType.INCOME && isIncomeCategory(category) -> category
                value == TransactionType.INCOME -> "工资"
                categoryBelongsTo(expenseGroup, category) -> category
                else -> defaultExpenseCategory(expenseGroup)
            },
            error = null,
        )
    }

    fun setAmount(value: String) = update {
        val clean = value.filter { it.isDigit() || it == '.' || it == ',' }.take(14)
        copy(amountText = clean, error = null)
    }

    fun setCategory(value: String) = update { copy(category = value, error = null) }
    fun setExpenseGroup(value: ExpenseGroup) = update {
        copy(
            expenseGroup = value,
            category = if (categoryBelongsTo(value, category)) category else defaultExpenseCategory(value),
            error = null,
        )
    }
    fun setNote(value: String) = update { copy(note = value.take(120), error = null) }

    fun setDate(value: LocalDate) = update {
        val old = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(occurredAt),
            ZoneId.systemDefault(),
        )
        val updated = value.atTime(old.toLocalTime())
        copy(occurredAt = updated.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    fun saveEditor(onSaved: () -> Unit = {}) {
        if (editorSaveInFlight) return
        val state = _editor.value
        val amount = parseMinor(state.amountText)
        if (amount == null || amount <= 0) {
            _editor.value = state.copy(error = "请输入大于 0 的欧元金额")
            return
        }
        val existing = transactions.value.firstOrNull { it.id == state.editingId }
        val now = System.currentTimeMillis()
        editorSaveInFlight = true
        viewModelScope.launch {
            try {
                repository.saveTransaction(
                    TransactionEntity(
                    id = state.editingId,
                    type = state.type,
                    originalAmountMinor = amount,
                    originalCurrency = CurrencyCode.EUR,
                    eurAmountMinor = amount,
                    exchangeRateMicros = 1_000_000,
                    category = state.category,
                    expenseGroup = state.expenseGroup,
                    occurredAt = state.occurredAt,
                    note = state.note.trim(),
                    inputMethod = state.inputMethod,
                    rawVoiceText = state.rawVoiceText,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    ),
                )
                debugLog("transaction.save", "id=${state.editingId}, type=${state.type}, eurMinor=$amount")
                _editor.value = EditorState()
                onSaved()
            } finally {
                editorSaveInFlight = false
            }
        }
    }

    fun deleteEditing(onDeleted: () -> Unit = {}) {
        val state = _editor.value
        val transaction = transactions.value.firstOrNull { it.id == state.editingId } ?: return
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            debugLog("transaction.delete", "id=${transaction.id}")
            _editor.value = EditorState()
            onDeleted()
        }
    }

    fun setBillingCloseDay(day: Int) {
        userPreferences.setBillingCloseDay(day)
        debugLog("billing.close_day", "day=${day.coerceIn(1, 31)}")
    }

    fun setBudgetSettings(settings: BudgetSettings) = userPreferences.setBudgetSettings(settings)

    fun openRecurringTemplate() {
        _recurringEditor.value = RecurringEditorState(visible = true)
    }

    fun editRecurringTemplate(template: RecurringTemplateEntity) {
        _recurringEditor.value = RecurringEditorState(
            visible = true,
            editingId = template.id,
            name = template.name,
            amountText = minorToInput(template.amountMinor),
            expenseGroup = template.expenseGroup,
            category = template.category,
            frequency = template.frequency,
            nextDueDate = LocalDate.ofEpochDay(template.nextDueEpochDay),
            note = template.note,
        )
    }

    fun dismissRecurringEditor() { _recurringEditor.value = RecurringEditorState() }
    fun setRecurringName(value: String) = updateRecurring { copy(name = value.take(40), error = null) }
    fun setRecurringAmount(value: String) = updateRecurring {
        copy(amountText = value.filter { it.isDigit() || it == '.' || it == ',' }.take(14), error = null)
    }
    fun setRecurringGroup(value: ExpenseGroup) = updateRecurring {
        copy(
            expenseGroup = value,
            category = if (categoryBelongsTo(value, category)) category else defaultExpenseCategory(value),
            error = null,
        )
    }
    fun setRecurringCategory(value: String) = updateRecurring { copy(category = value, error = null) }
    fun setRecurringFrequency(value: RecurrenceFrequency) = updateRecurring { copy(frequency = value, error = null) }
    fun setRecurringDate(value: LocalDate) = updateRecurring { copy(nextDueDate = value, error = null) }
    fun setRecurringNote(value: String) = updateRecurring { copy(note = value.take(120), error = null) }

    fun saveRecurringTemplate(onSaved: () -> Unit = {}) {
        if (recurringSaveInFlight) return
        val state = _recurringEditor.value
        val amount = parseMinor(state.amountText)
        when {
            state.name.isBlank() -> {
                _recurringEditor.value = state.copy(error = "请输入周期账目名称")
                return
            }
            amount == null || amount <= 0 -> {
                _recurringEditor.value = state.copy(error = "请输入大于 0 的欧元金额")
                return
            }
        }
        val existing = recurringTemplates.value.firstOrNull { it.id == state.editingId }
        val now = System.currentTimeMillis()
        recurringSaveInFlight = true
        viewModelScope.launch {
            try {
                repository.saveRecurringTemplate(
                    RecurringTemplateEntity(
                    id = state.editingId,
                    name = state.name.trim(),
                    amountMinor = amount,
                    expenseGroup = state.expenseGroup,
                    category = state.category,
                    frequency = state.frequency,
                    nextDueEpochDay = state.nextDueDate.toEpochDay(),
                    note = state.note.trim(),
                    enabled = existing?.enabled ?: true,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    ),
                )
                _recurringEditor.value = RecurringEditorState()
                onSaved()
            } finally {
                recurringSaveInFlight = false
            }
        }
    }

    fun deleteRecurringTemplate(template: RecurringTemplateEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteRecurringTemplate(template)
            onDeleted()
        }
    }

    private suspend fun synchronizeDueRecurringTemplates(templates: List<RecurringTemplateEntity>) {
        val today = LocalDate.now()
        templates.filter { it.enabled }.forEach { template ->
            var current = template
            var processed = 0
            while (LocalDate.ofEpochDay(current.nextDueEpochDay) <= today && processed < 120) {
                val dueDate = LocalDate.ofEpochDay(current.nextDueEpochDay)
                val now = System.currentTimeMillis()
                val advancedTemplate = current.copy(
                    nextDueEpochDay = followingRecurringDate(dueDate, current.frequency).toEpochDay(),
                    updatedAt = now,
                )
                val transaction = TransactionEntity(
                    type = TransactionType.EXPENSE,
                    originalAmountMinor = current.amountMinor,
                    originalCurrency = CurrencyCode.EUR,
                    eurAmountMinor = current.amountMinor,
                    exchangeRateMicros = 1_000_000,
                    category = current.category,
                    expenseGroup = current.expenseGroup,
                    occurredAt = dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    note = current.note.ifBlank { current.name },
                    inputMethod = InputMethod.RECURRING,
                    rawVoiceText = null,
                    createdAt = now,
                    updatedAt = now,
                )
                val posted = repository.postRecurringIfCurrent(
                    transaction = transaction,
                    advancedTemplate = advancedTemplate,
                    expectedDueEpochDay = current.nextDueEpochDay,
                )
                if (!posted) break
                debugLog("recurring.sync", "template=${current.id}, due=$dueDate")
                current = advancedTemplate
                processed += 1
            }
        }
    }

    fun exportBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    LocalDataTransfer.writeBackup(
                        context, uri, transactions.value, recurringTemplates.value,
                        billingCloseDay.value, budgetSettings.value,
                    )
                }
            }
            onResult(result.isSuccess, result.exceptionOrNull()?.message ?: "备份已导出")
        }
    }

    fun exportCsv(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { LocalDataTransfer.writeCsv(context, uri, transactions.value) }
            }
            onResult(result.isSuccess, result.exceptionOrNull()?.message ?: "CSV 已导出")
        }
    }

    fun restoreBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val payload = withContext(Dispatchers.IO) { LocalDataTransfer.readBackup(context, uri) }
                repository.replaceAll(payload.transactions, payload.recurringTemplates)
                userPreferences.setBillingCloseDay(payload.billingCloseDay)
                userPreferences.setBudgetSettings(payload.budgetSettings)
            }
            onResult(result.isSuccess, result.exceptionOrNull()?.message ?: "备份已恢复")
        }
    }

    fun insertDebugData(onResult: (Boolean) -> Unit) {
        if (!BuildConfig.DEBUG || transactions.value.isNotEmpty()) {
            onResult(false)
            return
        }
        val now = LocalDateTime.now().withSecond(0).withNano(0)
        val createdAt = System.currentTimeMillis()
        fun entry(
            daysAgo: Long,
            hour: Int,
            type: TransactionType,
            amountMinor: Long,
            category: String,
            note: String,
            expenseGroup: ExpenseGroup = ExpenseGroup.DAILY,
        ) = TransactionEntity(
            type = type,
            originalAmountMinor = amountMinor,
            originalCurrency = CurrencyCode.EUR,
            eurAmountMinor = amountMinor,
            exchangeRateMicros = 1_000_000,
            category = category,
            expenseGroup = expenseGroup,
            occurredAt = now.minusDays(daysAgo).withHour(hour).withMinute(15)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            note = note,
            inputMethod = InputMethod.MANUAL,
            rawVoiceText = null,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
        val demo = listOf(
            entry(0, 8, TransactionType.EXPENSE, 420, "餐饮", "晨间咖啡"),
            entry(0, 12, TransactionType.EXPENSE, 1280, "餐饮", "工作日午饭"),
            entry(1, 18, TransactionType.EXPENSE, 2460, "购物", "超市补给"),
            entry(2, 9, TransactionType.EXPENSE, 1713, "影音订阅", "线上订阅", ExpenseGroup.SUBSCRIPTION),
            entry(3, 19, TransactionType.EXPENSE, 1500, "餐饮", "和朋友吃饭"),
            entry(4, 8, TransactionType.EXPENSE, 490, "交通", "地铁周票补差"),
            entry(5, 20, TransactionType.EXPENSE, 1450, "娱乐", "电影票"),
            entry(1, 7, TransactionType.EXPENSE, 3800, "保险", "家庭保险", ExpenseGroup.FIXED),
            entry(4, 10, TransactionType.EXPENSE, 10000, "定投", "ETF 定投", ExpenseGroup.INVESTMENT),
            entry(6, 16, TransactionType.EXPENSE, 3200, "健身会员", "健身房", ExpenseGroup.SUBSCRIPTION),
            entry(8, 10, TransactionType.EXPENSE, 857, "礼物", "家人礼物"),
            entry(10, 7, TransactionType.EXPENSE, 6890, "旅行", "火车票"),
            entry(12, 12, TransactionType.INCOME, 320000, "工资", "本月工资"),
        )
        viewModelScope.launch {
            repository.insertTransactions(demo)
            debugLog("debug.seed", "inserted=${demo.size}")
            onResult(true)
        }
    }

    fun clearDebugData(onCleared: () -> Unit = {}) {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            repository.clearTransactions()
            debugLog("debug.clear", "all transactions deleted")
            onCleared()
        }
    }

    private inline fun update(block: EditorState.() -> EditorState) {
        _editor.value = _editor.value.block()
    }

    private inline fun updateRecurring(block: RecurringEditorState.() -> RecurringEditorState) {
        _recurringEditor.value = _recurringEditor.value.block()
    }

    private fun debugLog(event: String, details: String) {
        if (BuildConfig.DEBUG) Log.d("YiBiDebug", "$event | $details")
    }

    class Factory(
        private val repository: LedgerRepository,
        private val userPreferences: UserPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LedgerViewModel(repository, userPreferences) as T
    }
}
