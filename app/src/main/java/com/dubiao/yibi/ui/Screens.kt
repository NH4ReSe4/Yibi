@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.dubiao.yibi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dubiao.yibi.data.CurrencyCode
import com.dubiao.yibi.data.BudgetSettings
import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.RecurringTemplateEntity
import com.dubiao.yibi.data.TransactionEntity
import com.dubiao.yibi.data.TransactionType
import com.dubiao.yibi.domain.formatMoney
import com.dubiao.yibi.domain.billingCycleFor
import com.dubiao.yibi.domain.parseMinor
import com.dubiao.yibi.domain.weeklyFlexibleAllowance
import com.dubiao.yibi.BuildConfig
import com.dubiao.yibi.ui.theme.Apricot
import com.dubiao.yibi.ui.theme.Coral
import com.dubiao.yibi.ui.theme.Forest
import com.dubiao.yibi.ui.theme.ForestSoft
import com.dubiao.yibi.ui.theme.Hairline
import com.dubiao.yibi.ui.theme.Mint
import com.dubiao.yibi.ui.theme.Muted
import com.dubiao.yibi.ui.theme.Paper
import com.dubiao.yibi.update.AppUpdateState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun HomeScreen(
    transactions: List<TransactionEntity>,
    billingCloseDay: Int,
    budgetSettings: BudgetSettings,
    isListening: Boolean,
    onManual: () -> Unit,
    onVoice: () -> Unit,
    onSeeAll: () -> Unit,
    onTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cycle = remember(billingCloseDay) { billingCycleFor(LocalDate.now(), billingCloseDay) }
    val cycleTransactions = remember(transactions, cycle) {
        transactions.filter { it.localDate() in cycle.start..cycle.endInclusive }
    }
    val expense = cycleTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.eurAmountMinor }
    val income = cycleTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.eurAmountMinor }
    val currentWeekStart = maxOf(LocalDate.now().startOfWeek(), cycle.start)
    val weeklyFlexibleSpent = cycleTransactions.filter {
        it.type == TransactionType.EXPENSE && it.expenseGroup == ExpenseGroup.DAILY &&
            it.localDate() in currentWeekStart..LocalDate.now()
    }.sumOf { it.eurAmountMinor }
    val weeklyFlexibleLimit = weeklyFlexibleAllowance(budgetSettings, cycle.start, cycle.endInclusive)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 18.dp, 20.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column {
                Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = Muted)
                Text("今天，记一笔", style = MaterialTheme.typography.headlineLarge)
            }
        }
        item { MonthSummaryCard(expense, income) }
        if (budgetSettings.totalMinor > 0L) {
            item {
                BudgetOverviewCard(
                    spent = expense,
                    budget = budgetSettings.totalMinor,
                    weeklySpent = weeklyFlexibleSpent,
                    weeklyLimit = weeklyFlexibleLimit,
                )
            }
        }
        item {
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickAction(
                    title = "手动记账",
                    subtitle = "逐项填写，准确记录",
                    icon = Icons.Filled.Add,
                    container = Mint,
                    content = Forest,
                    onClick = onManual,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                QuickAction(
                    title = if (isListening) "正在聆听" else "语音记账",
                    subtitle = if (isListening) "轻触结束语音输入" else "说出金额、用途和日期",
                    icon = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    container = if (isListening) Color(0xFFF7DED8) else Apricot.copy(alpha = .55f),
                    content = if (isListening) Coral else Color(0xFF704817),
                    onClick = onVoice,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        item {
            SectionTitle("最近记录", if (transactions.isEmpty()) null else "查看全部", onSeeAll)
        }
        if (transactions.isEmpty()) {
            item { EmptyLedger(onManual) }
        } else {
            items(transactions.take(5), key = { it.id }) { transaction ->
                TransactionRow(transaction, onClick = { onTransaction(transaction) })
            }
        }
    }
}

@Composable
private fun BudgetOverviewCard(spent: Long, budget: Long, weeklySpent: Long, weeklyLimit: Long) {
    val progress = (spent.toFloat() / budget.coerceAtLeast(1L)).coerceIn(0f, 1f)
    val remaining = budget - spent
    Surface(color = Paper, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Savings, contentDescription = null, tint = Forest)
                Spacer(Modifier.width(10.dp))
                Text("本周期预算", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    if (remaining >= 0) "还可用 ${formatMoney(remaining, CurrencyCode.EUR)}"
                    else "已超出 ${formatMoney(-remaining, CurrencyCode.EUR)}",
                    color = if (remaining >= 0) Muted else Coral,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (remaining >= 0) Forest else Coral,
                trackColor = Mint,
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(8.dp))
            Text("已用 ${formatMoney(spent, CurrencyCode.EUR)} / ${formatMoney(budget, CurrencyCode.EUR)}", color = Muted, style = MaterialTheme.typography.labelMedium)
            if (weeklyLimit > 0L) {
                val weeklyRemaining = weeklyLimit - weeklySpent
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("本周自由消费", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${formatMoney(weeklySpent, CurrencyCode.EUR)} / ${formatMoney(weeklyLimit, CurrencyCode.EUR)}",
                            color = Muted,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Surface(
                        color = if (weeklyRemaining >= 0) Mint else Color(0xFFF7DED8),
                        contentColor = if (weeklyRemaining >= 0) Forest else Coral,
                        shape = CircleShape,
                    ) {
                        Text(
                            if (weeklyRemaining >= 0) "剩余 ${formatMoney(weeklyRemaining, CurrencyCode.EUR)}"
                            else "超出 ${formatMoney(-weeklyRemaining, CurrencyCode.EUR)}",
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(expense: Long, income: Long) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Forest),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("本账单周期结余", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Surface(color = Color.White.copy(alpha = .1f), shape = CircleShape) {
                    Text("EUR", color = Color.White.copy(alpha = .9f), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(formatMoney(income - expense, CurrencyCode.EUR), color = Color.White, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(22.dp))
            Row {
                SummaryMetric("收入", formatMoney(income, CurrencyCode.EUR), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(38.dp).background(Color.White.copy(alpha = .18f)))
                SummaryMetric("支出", formatMoney(expense, CurrencyCode.EUR), Modifier.weight(1f).padding(start = 20.dp))
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = .62f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = container,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(17.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Paper.copy(alpha = .65f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = content)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = .72f))
        }
    }
}

@Composable
private fun EmptyLedger(onAdd: () -> Unit) {
    Surface(color = Paper, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.BarChart, contentDescription = null, tint = Forest)
            }
            Spacer(Modifier.height(12.dp))
            Text("从第一笔开始", style = MaterialTheme.typography.titleMedium)
            Text("所有账目仅保存在本机", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onAdd) { Text("记一笔") }
        }
    }
}

@Composable
fun LedgerScreen(
    transactions: List<TransactionEntity>,
    onAdd: () -> Unit,
    onTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf<TransactionType?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var groupFilter by rememberSaveable { mutableStateOf<ExpenseGroup?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var fromEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var toEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var minAmount by rememberSaveable { mutableStateOf("") }
    var maxAmount by rememberSaveable { mutableStateOf("") }
    var pickingStart by remember { mutableStateOf<Boolean?>(null) }
    val minMinor = parseMinor(minAmount)
    val maxMinor = parseMinor(maxAmount)
    val hasActiveFilters = filter != null || query.isNotBlank() || groupFilter != null || categoryFilter != null ||
        fromEpochDay != null || toEpochDay != null || minAmount.isNotBlank() || maxAmount.isNotBlank()
    val clearAllFilters = {
        filter = null
        query = ""
        groupFilter = null
        categoryFilter = null
        fromEpochDay = null
        toEpochDay = null
        minAmount = ""
        maxAmount = ""
        showAdvanced = false
    }
    val filtered = transactions.filter { entry ->
        val needle = query.trim()
        val groupName = expenseGroups.firstOrNull { it.group == entry.expenseGroup }?.label.orEmpty()
        val matchesQuery = needle.isBlank() || listOf(entry.note, entry.category, groupName).any { it.contains(needle, ignoreCase = true) }
        val epochDay = entry.localDate().toEpochDay()
        (filter == null || entry.type == filter) && matchesQuery &&
            (groupFilter == null || entry.expenseGroup == groupFilter) &&
            (categoryFilter == null || entry.category == categoryFilter) &&
            (fromEpochDay == null || epochDay >= fromEpochDay!!) &&
            (toEpochDay == null || epochDay <= toEpochDay!!) &&
            (minMinor == null || entry.eurAmountMinor >= minMinor) &&
            (maxMinor == null || entry.eurAmountMinor <= maxMinor)
    }
    val groups = filtered.groupBy { it.localDate() }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 18.dp, 20.dp, 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("流水", style = MaterialTheme.typography.headlineLarge)
                Text("按时间查看每一笔收支", color = Muted)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("全部", filter == null) { filter = null }
                    FilterChip("支出", filter == TransactionType.EXPENSE) { filter = TransactionType.EXPENSE }
                    FilterChip("收入", filter == TransactionType.INCOME) { filter = TransactionType.INCOME }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("搜索名称、备注或分类") },
                    trailingIcon = {
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) "收起" else "筛选")
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                )
                if (showAdvanced) {
                    Spacer(Modifier.height(12.dp))
                    Surface(color = Paper, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("开销大类", color = Muted, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip("不限", groupFilter == null) { groupFilter = null; categoryFilter = null }
                                expenseGroups.forEach { group ->
                                    FilterChip(group.label, groupFilter == group.group) { groupFilter = group.group; categoryFilter = null }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("小类", color = Muted, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            val availableCategories = transactions
                                .filter { groupFilter == null || it.expenseGroup == groupFilter }
                                .map { it.category }.distinct().sorted()
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip("不限", categoryFilter == null) { categoryFilter = null }
                                availableCategories.forEach { category ->
                                    FilterChip(category, categoryFilter == category) { categoryFilter = category }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("日期范围", color = Muted, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DateFilterButton("开始", fromEpochDay) { pickingStart = true }
                                DateFilterButton("结束", toEpochDay) { pickingStart = false }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("金额范围（欧元）", color = Muted, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AmountFilterField("最低", minAmount, { minAmount = it }, Modifier.weight(1f))
                                AmountFilterField("最高", maxAmount, { maxAmount = it }, Modifier.weight(1f))
                            }
                            if (groupFilter != null || categoryFilter != null || fromEpochDay != null || toEpochDay != null || minAmount.isNotBlank() || maxAmount.isNotBlank()) {
                                TextButton(onClick = {
                                    groupFilter = null; categoryFilter = null; fromEpochDay = null; toEpochDay = null
                                    minAmount = ""; maxAmount = ""
                                }) { Text("清除高级筛选") }
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    if (transactions.isEmpty()) EmptyLedger(onAdd)
                    else if (hasActiveFilters) EmptyFilterResult(clearAllFilters)
                    else EmptyLedger(onAdd)
                }
            } else {
                groups.forEach { (date, entries) ->
                    item(key = "date-$date") {
                        Text(dateTitle(date), color = Muted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(entries, key = { it.id }) { entry ->
                        TransactionRow(entry) { onTransaction(entry) }
                    }
                }
            }
        }
    }

    pickingStart?.let { isStart ->
        val initial = (if (isStart) fromEpochDay else toEpochDay) ?: LocalDate.now().toEpochDay()
        val picker = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(initial).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { pickingStart = null },
            dismissButton = { TextButton(onClick = { pickingStart = null }) { Text("取消") } },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay()
                        if (isStart) {
                            fromEpochDay = day
                            if (toEpochDay != null && day > toEpochDay!!) toEpochDay = day
                        } else {
                            toEpochDay = day
                            if (fromEpochDay != null && day < fromEpochDay!!) fromEpochDay = day
                        }
                    }
                    pickingStart = null
                }) { Text("确定") }
            },
        ) { androidx.compose.material3.DatePicker(picker) }
    }
}

@Composable
private fun EmptyFilterResult(onClear: () -> Unit) {
    Surface(color = Paper, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = Forest, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(10.dp))
            Text("没有符合条件的记录", style = MaterialTheme.typography.titleMedium)
            Text("可以调整关键词、日期、分类或金额范围", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClear) { Text("清除全部筛选") }
        }
    }
}

@Composable
private fun RowScope.DateFilterButton(label: String, epochDay: Long?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            epochDay?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("yyyy/M/d")) } ?: label,
            modifier = Modifier.padding(13.dp),
            color = if (epochDay == null) Muted else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AmountFilterField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(14)) },
        modifier = modifier,
        label = { Text(label) },
        prefix = { Text("€") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Forest else Paper,
        contentColor = if (selected) Color.White else Muted,
        shape = CircleShape,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TransactionRow(transaction: TransactionEntity, onClick: () -> Unit) {
    val category = categoryUi(transaction.category)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Paper,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(category.color), contentAlignment = Alignment.Center) {
                Icon(category.icon, contentDescription = null, tint = category.tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.note.ifBlank { transaction.category }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(transaction.category, color = Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val signed = if (transaction.type == TransactionType.EXPENSE) -transaction.eurAmountMinor else transaction.eurAmountMinor
                Text(
                    formatMoney(signed, CurrencyCode.EUR, signed = true),
                    color = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.onSurface else ForestSoft,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(transaction.localTime(), color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ReportsScreen(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier,
) {
    var period by rememberSaveable { mutableStateOf(ReportPeriod.WEEK) }
    val expenses = remember(transactions, period) {
        reportExpenses(transactions, period)
    }
    val total = expenses.sumOf { it.eurAmountMinor }
    val groupTotals = expenseGroups.map { group ->
        group to expenses.filter { it.expenseGroup == group.group }.sumOf { it.eurAmountMinor }
    }
    val chart = remember(expenses, period) { reportChart(expenses, period) }
    val rangeLabel = remember(period) { reportRangeLabel(period) }
    val comparison = remember(transactions, period) { reportComparison(transactions, period) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 18.dp, 20.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("报表", style = MaterialTheme.typography.headlineLarge)
            Text("所有金额均以欧元记录", color = Muted)
        }
        item { ReportPeriodSelector(period, onSelected = { period = it }) }
        item {
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Paper), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(period.totalTitle, color = Muted, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.weight(1f))
                        Text(rangeLabel, color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                    Text(formatMoney(total, CurrencyCode.EUR), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(22.dp))
                    ReportBarChart(
                        chart,
                        Modifier.fillMaxWidth().height(if (period == ReportPeriod.WEEK) 166.dp else 150.dp),
                    )
                }
            }
        }
        item { ComparisonCard(comparison, period) }
        item { SectionTitle(period.groupTitle, null) {} }
        if (expenses.isEmpty()) {
            item { EmptyReport() }
        } else {
            item { ExpenseGroupOverview(groupTotals, total) }
            item { SectionTitle("支出占比", null) {} }
            item { ExpenseDonutChart(groupTotals, total) }
        }
    }
}

private data class CashFlowSummary(val income: Long, val expense: Long) {
    val net: Long get() = income - expense
}

private data class ReportComparison(val current: CashFlowSummary, val previous: CashFlowSummary)

private fun reportComparison(transactions: List<TransactionEntity>, period: ReportPeriod): ReportComparison {
    val today = LocalDate.now()
    val currentStart = when (period) {
        ReportPeriod.DAY -> today
        ReportPeriod.WEEK -> today.startOfWeek()
        ReportPeriod.MONTH -> today.withDayOfMonth(1)
    }
    val days = java.time.temporal.ChronoUnit.DAYS.between(currentStart, today)
    val previousEnd = currentStart.minusDays(1)
    val previousStart = previousEnd.minusDays(days)
    fun summarize(start: LocalDate, end: LocalDate): CashFlowSummary {
        val entries = transactions.filter { it.localDate() in start..end }
        return CashFlowSummary(
            income = entries.filter { it.type == TransactionType.INCOME }.sumOf { it.eurAmountMinor },
            expense = entries.filter { it.type == TransactionType.EXPENSE }.sumOf { it.eurAmountMinor },
        )
    }
    return ReportComparison(summarize(currentStart, today), summarize(previousStart, previousEnd))
}

@Composable
private fun ComparisonCard(comparison: ReportComparison, period: ReportPeriod) {
    val delta = comparison.current.expense - comparison.previous.expense
    val comparisonLabel = when (period) {
        ReportPeriod.DAY -> "较昨日"
        ReportPeriod.WEEK -> "较上周同期"
        ReportPeriod.MONTH -> "较上月同期"
    }
    val deltaText = when {
        comparison.previous.expense == 0L && comparison.current.expense == 0L -> "$comparisonLabel 持平"
        comparison.previous.expense == 0L -> "$comparisonLabel 新增 ${formatMoney(comparison.current.expense, CurrencyCode.EUR)}"
        else -> {
            val percent = kotlin.math.abs(delta).toDouble() / comparison.previous.expense * 100
            "$comparisonLabel ${if (delta >= 0) "增加" else "减少"} ${String.format(Locale.SIMPLIFIED_CHINESE, "%.1f%%", percent)}"
        }
    }
    Surface(color = Paper, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("本期收支", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(deltaText, color = if (delta > 0) Coral else ForestSoft, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ComparisonMetric("收入", comparison.current.income, Modifier.weight(1f))
                ComparisonMetric("支出", comparison.current.expense, Modifier.weight(1f))
                ComparisonMetric("结余", comparison.current.net, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text("上期同期支出 ${formatMoney(comparison.previous.expense, CurrencyCode.EUR)}", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ComparisonMetric(label: String, amount: Long, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
        Text(formatMoney(amount, CurrencyCode.EUR), style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun ExpenseGroupOverview(
    totals: List<Pair<ExpenseGroupUi, Long>>,
    total: Long,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        totals.chunked(2).forEach { rowGroups ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowGroups.forEach { (group, amount) ->
                    val ratio = if (total == 0L) 0f else (amount.toDouble() / total).toFloat()
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Paper,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(group.color),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(group.icon, contentDescription = null, tint = group.tint, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(9.dp))
                                Text(group.label, style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(formatMoney(amount, CurrencyCode.EUR), style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${formatPercentage(ratio)} · ${group.description}",
                                color = Muted,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseDonutChart(
    totals: List<Pair<ExpenseGroupUi, Long>>,
    total: Long,
) {
    val visibleTotals = totals.filter { it.second > 0L }
    Surface(color = Paper, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.size(146.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    visibleTotals.forEach { (group, amount) ->
                        val sweep = 360f * amount.toFloat() / total.coerceAtLeast(1L).toFloat()
                        drawArc(
                            color = group.tint,
                            startAngle = startAngle,
                            sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                            useCenter = true,
                        )
                        startAngle += sweep
                    }
                    drawCircle(color = Paper, radius = size.minDimension * .29f)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("总支出", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text(formatMoney(total, CurrencyCode.EUR), style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                totals.forEach { (group, amount) ->
                    val ratio = if (total == 0L) 0f else (amount.toDouble() / total).toFloat()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).background(group.tint, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(group.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text(formatPercentage(ratio), color = group.tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportPeriodSelector(selected: ReportPeriod, onSelected: (ReportPeriod) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(4.dp)) {
            ReportPeriod.entries.forEach { period ->
                Surface(
                    modifier = Modifier.weight(1f).clickable { onSelected(period) },
                    color = if (selected == period) Forest else Color.Transparent,
                    contentColor = if (selected == period) Color.White else Muted,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = if (selected == period) 1.dp else 0.dp,
                ) {
                    Text(
                        period.label,
                        modifier = Modifier.padding(vertical = 11.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportBarChart(data: List<ReportBarPoint>, modifier: Modifier) {
    val maxValue = max(1L, data.maxOfOrNull { it.amount } ?: 1L)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val gap = 12.dp.toPx()
            val barWidth = (size.width - gap * (data.size - 1)) / data.size
            data.forEachIndexed { index, point ->
                val amount = point.amount
                val height = if (amount == 0L) 4.dp.toPx() else size.height * amount / maxValue
                drawRoundRect(
                    color = if (point.emphasized) Forest else Mint,
                    topLeft = Offset(index * (barWidth + gap), size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            data.forEach { point ->
                Text(
                    point.label,
                    modifier = Modifier.weight(1f).padding(horizontal = 1.dp),
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun CategoryProgress(category: String, amount: Long, total: Long) {
    val ui = categoryUi(category)
    val ratio = if (total == 0L) 0f else (amount.toDouble() / total).toFloat().coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(ui.color), contentAlignment = Alignment.Center) {
            Icon(ui.icon, contentDescription = null, tint = ui.tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(category, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Text(formatPercentage(ratio), color = ui.tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text(formatMoney(amount, CurrencyCode.EUR), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { ratio },
                color = ui.tint,
                trackColor = ui.color,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            )
        }
    }
}

@Composable
private fun EmptyReport() {
    Surface(color = Paper, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.BarChart, contentDescription = null, tint = Forest, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(10.dp))
            Text("记录几笔后，这里会显示支出趋势")
        }
    }
}

@Composable
fun SettingsScreen(
    billingCloseDay: Int,
    budgetSettings: BudgetSettings,
    recurringTemplates: List<RecurringTemplateEntity>,
    onBillingCloseDay: (Int) -> Unit,
    onEditBudget: () -> Unit,
    onAddRecurring: () -> Unit,
    onEditRecurring: (RecurringTemplateEntity) -> Unit,
    onDeleteRecurring: (RecurringTemplateEntity) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onExportCsv: () -> Unit,
    updateState: AppUpdateState,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 18.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("设置", style = MaterialTheme.typography.headlineLarge)
            Text("管理账单周期与本地数据", color = Muted)
        }
        item {
            BillingCycleCard(
                closeDay = billingCloseDay,
                onCloseDay = onBillingCloseDay,
            )
        }
        item { BudgetSettingsCard(budgetSettings, onEditBudget) }
        item {
            RecurringSettingsCard(
                templates = recurringTemplates,
                onAdd = onAddRecurring,
                onEdit = onEditRecurring,
                onDelete = onDeleteRecurring,
            )
        }
        item { DataTransferCard(onExportBackup, onRestoreBackup, onExportCsv) }
        item { AppUpdateCard(updateState, onCheckUpdate, onDownloadUpdate, onInstallUpdate) }
    }
}

@Composable
private fun AppUpdateCard(
    state: AppUpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val description = when (state) {
        AppUpdateState.Idle -> "当前版本 ${BuildConfig.VERSION_NAME} · 手动检查"
        AppUpdateState.Checking -> "正在检查新版本…"
        AppUpdateState.UpToDate -> "当前版本 ${BuildConfig.VERSION_NAME}，已是最新版"
        is AppUpdateState.Available -> "发现 ${state.info.versionName}：${state.info.releaseNotes.ifBlank { "有可用更新" }}"
        is AppUpdateState.Downloading -> state.percent?.let { "正在下载 ${state.info.versionName} · $it%" }
            ?: "正在下载 ${state.info.versionName}…"
        is AppUpdateState.Ready -> "${state.info.versionName} 已下载，等待系统安装"
        is AppUpdateState.Failed -> state.message
    }
    val action = when (state) {
        AppUpdateState.Idle, AppUpdateState.UpToDate, is AppUpdateState.Failed -> "检查更新"
        is AppUpdateState.Available -> "下载"
        is AppUpdateState.Ready -> "安装"
        AppUpdateState.Checking, is AppUpdateState.Downloading -> null
    }
    Surface(color = Paper, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Download, contentDescription = null, tint = Forest)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("应用更新", style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        color = if (state is AppUpdateState.Failed) Coral else Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (action != null) {
                    Spacer(Modifier.width(10.dp))
                    TextButton(
                        onClick = when (state) {
                            is AppUpdateState.Available -> onDownload
                            is AppUpdateState.Ready -> onInstall
                            else -> onCheck
                        },
                    ) { Text(action) }
                }
            }
            if (state == AppUpdateState.Checking) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Forest, trackColor = Mint)
            }
            if (state is AppUpdateState.Downloading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (state.percent ?: 0) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Forest,
                    trackColor = Mint,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("简体中文 · 统一货币：欧元", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun BudgetSettingsCard(settings: BudgetSettings, onEdit: () -> Unit) {
    val configured = listOf(
        "总预算" to settings.totalMinor,
        "固定" to settings.fixedMinor,
        "固定订阅" to settings.subscriptionMinor,
        "自由消费" to settings.dailyMinor,
        "投资" to settings.investmentMinor,
    ).filter { it.second > 0L }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        color = Paper,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Savings, contentDescription = null, tint = Forest)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("账单周期预算", style = MaterialTheme.typography.titleMedium)
                    Text(if (configured.isEmpty()) "尚未设置" else "固定预留与弹性分配", color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Text("设置", color = Forest, style = MaterialTheme.typography.labelLarge)
            }
            if (configured.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    configured.forEach { (label, amount) ->
                        Surface(color = Mint, shape = CircleShape) {
                            Text("$label ${formatMoney(amount, CurrencyCode.EUR)}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Forest, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringSettingsCard(
    templates: List<RecurringTemplateEntity>,
    onAdd: () -> Unit,
    onEdit: (RecurringTemplateEntity) -> Unit,
    onDelete: (RecurringTemplateEntity) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<RecurringTemplateEntity?>(null) }
    Surface(color = Paper, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.EventRepeat, contentDescription = null, tint = Forest)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("周期账目", style = MaterialTheme.typography.titleMedium)
                    Text("房租、订阅等将在到期日自动写入流水", color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onAdd) { Text("添加") }
            }
            templates.forEach { template ->
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(template) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(template.name, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${frequencyLabel(template.frequency)} · ${LocalDate.ofEpochDay(template.nextDueEpochDay).format(DateTimeFormatter.ofPattern("yyyy/M/d"))} · ${formatMoney(template.amountMinor, CurrencyCode.EUR)}",
                            color = Muted,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    FilledTonalIconButton(onClick = { pendingDelete = template }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除周期账目", tint = Coral)
                    }
                }
            }
            if (templates.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("还没有周期账目模板", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    pendingDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除“${template.name}”？") },
            text = { Text("只会删除模板，已记入的流水不会受影响。") },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null; onDelete(template) }) { Text("删除", color = Coral) }
            },
        )
    }
}

@Composable
private fun DataTransferCard(onBackup: () -> Unit, onRestore: () -> Unit, onCsv: () -> Unit) {
    Surface(color = Paper, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Download, contentDescription = null, tint = Forest)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("备份与导出", style = MaterialTheme.typography.titleMedium)
                    Text("文件由你选择保存位置，全程离线", color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBackup, shape = CircleShape) { Icon(Icons.Outlined.Download, null); Spacer(Modifier.width(6.dp)); Text("完整备份") }
                Button(onClick = onRestore, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Forest)) { Icon(Icons.Outlined.Upload, null); Spacer(Modifier.width(6.dp)); Text("恢复备份") }
                TextButton(onClick = onCsv) { Text("导出 CSV") }
            }
        }
    }
}

@Composable
private fun BillingCycleCard(
    closeDay: Int,
    onCloseDay: (Int) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val cycle = remember(closeDay) { billingCycleFor(LocalDate.now(), closeDay) }
    val formatter = remember { DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE) }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { showPicker = true },
        color = Paper,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Mint, shape = CircleShape) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Forest)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("当前账单周期", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${cycle.start.format(formatter)}–${cycle.endInclusive.format(formatter)}",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(color = Forest, contentColor = Color.White, shape = CircleShape) {
                Text(
                    "设置截止日",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            containerColor = Paper,
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 30.dp)) {
                Text("选择账单截止日", style = MaterialTheme.typography.headlineMedium)
                Text("截止日次日将自动进入新的账单周期", color = Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(20.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 7,
                ) {
                    (1..31).forEach { day ->
                        val selected = day == closeDay
                        Surface(
                            modifier = Modifier.size(40.dp).clickable {
                                onCloseDay(day)
                                showPicker = false
                            },
                            color = if (selected) Forest else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) Color.White else Muted,
                            shape = CircleShape,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(day.toString(), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("若当月没有所选日期（如 2 月 31 日），将以当月最后一天为截止日。", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String?, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onAction) {
                Text(action)
                Spacer(Modifier.width(3.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun List<TransactionEntity>.currentMonth(): List<TransactionEntity> {
    val now = LocalDate.now()
    return filter {
        val date = it.localDate()
        date.year == now.year && date.month == now.month
    }
}

private fun TransactionEntity.localDate(): LocalDate = Instant.ofEpochMilli(occurredAt)
    .atZone(ZoneId.systemDefault()).toLocalDate()

private fun TransactionEntity.localTime(): String = Instant.ofEpochMilli(occurredAt)
    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))

private enum class ReportPeriod(
    val label: String,
    val totalTitle: String,
    val groupTitle: String,
) {
    DAY("日", "近 7 天支出", "近 7 天支出构成"),
    WEEK("周", "近 7 周支出", "近 7 周支出构成"),
    MONTH("月", "近 7 个月支出", "近 7 个月支出构成"),
}

private data class ReportBarPoint(
    val label: String,
    val amount: Long,
    val emphasized: Boolean,
)

private fun reportExpenses(
    transactions: List<TransactionEntity>,
    period: ReportPeriod,
): List<TransactionEntity> {
    val today = LocalDate.now()
    val start = when (period) {
        ReportPeriod.DAY -> today.minusDays(6)
        ReportPeriod.WEEK -> today.startOfWeek().minusWeeks(6)
        ReportPeriod.MONTH -> today.withDayOfMonth(1).minusMonths(6)
    }
    return transactions.filter { transaction ->
        if (transaction.type != TransactionType.EXPENSE) return@filter false
        val date = transaction.localDate()
        date in start..today
    }
}

private fun reportChart(
    expenses: List<TransactionEntity>,
    period: ReportPeriod,
): List<ReportBarPoint> {
    val today = LocalDate.now()
    return when (period) {
        ReportPeriod.WEEK -> (6 downTo 0).map { offset ->
            val weekStart = today.startOfWeek().minusWeeks(offset.toLong())
            val weekEnd = weekStart.plusDays(6)
            ReportBarPoint(
                label = "${weekStart.monthValue}/${weekStart.dayOfMonth}\n–${weekEnd.monthValue}/${weekEnd.dayOfMonth}",
                amount = expenses.filter { it.localDate() in weekStart..weekEnd }.sumOf { it.eurAmountMinor },
                emphasized = offset == 0,
            )
        }

        ReportPeriod.DAY -> (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            ReportBarPoint(
                label = "${date.monthValue}/${date.dayOfMonth}",
                amount = expenses.filter { it.localDate() == date }.sumOf { it.eurAmountMinor },
                emphasized = offset == 0,
            )
        }

        ReportPeriod.MONTH -> (6 downTo 0).map { offset ->
            val monthStart = today.withDayOfMonth(1).minusMonths(offset.toLong())
            ReportBarPoint(
                label = "${monthStart.monthValue}月",
                amount = expenses.filter {
                    val date = it.localDate()
                    date.year == monthStart.year && date.month == monthStart.month
                }.sumOf { it.eurAmountMinor },
                emphasized = offset == 0,
            )
        }
    }
}

private fun formatPercentage(ratio: Float): String =
    String.format(Locale.SIMPLIFIED_CHINESE, "%.1f%%", ratio * 100f)

private fun reportRangeLabel(period: ReportPeriod): String {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE)
    return when (period) {
        ReportPeriod.DAY -> "${today.minusDays(6).format(formatter)}–${today.format(formatter)}"
        ReportPeriod.WEEK -> "${today.startOfWeek().minusWeeks(6).format(formatter)}–${today.format(formatter)}"
        ReportPeriod.MONTH -> {
            val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.SIMPLIFIED_CHINESE)
            "${today.withDayOfMonth(1).minusMonths(6).format(monthFormatter)}–${today.format(monthFormatter)}"
        }
    }
}

private fun LocalDate.startOfWeek(): LocalDate = minusDays((dayOfWeek.value - 1).toLong())

private fun dateTitle(date: LocalDate): String = when (date) {
    LocalDate.now() -> "今天"
    LocalDate.now().minusDays(1) -> "昨天"
    else -> date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE))
}

private fun greeting(): String = when (LocalDateTime.now().hour) {
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}
