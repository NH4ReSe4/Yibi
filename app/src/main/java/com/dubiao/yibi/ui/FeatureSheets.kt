@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.dubiao.yibi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dubiao.yibi.data.BudgetSettings
import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.RecurrenceFrequency
import com.dubiao.yibi.domain.minorToInput
import com.dubiao.yibi.domain.parseMinor
import com.dubiao.yibi.domain.billingCycleFor
import com.dubiao.yibi.domain.formatMoney
import com.dubiao.yibi.domain.linkedBudgetSettings
import com.dubiao.yibi.domain.weeklyFlexibleAllowance
import com.dubiao.yibi.data.CurrencyCode
import com.dubiao.yibi.ui.theme.Forest
import com.dubiao.yibi.ui.theme.Muted
import com.dubiao.yibi.ui.theme.Paper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RecurringTemplateSheet(
    state: RecurringEditorState,
    onDismiss: () -> Unit,
    onName: (String) -> Unit,
    onAmount: (String) -> Unit,
    onGroup: (ExpenseGroup) -> Unit,
    onCategory: (String) -> Unit,
    onFrequency: (RecurrenceFrequency) -> Unit,
    onDate: (LocalDate) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Paper) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(if (state.editingId == 0L) "添加周期账目" else "编辑周期账目", style = MaterialTheme.typography.headlineMedium)
            Text("到期后会在首页提醒，由你确认后入账", color = Muted)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = state.name, onValueChange = onName, modifier = Modifier.fillMaxWidth(),
                label = { Text("名称") }, placeholder = { Text("例如：房租、Netflix") }, singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.amountText, onValueChange = onAmount, modifier = Modifier.fillMaxWidth(),
                label = { Text("金额（欧元）") }, leadingIcon = { Text("€") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text("开销大类", color = Muted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                expenseGroups.forEach { group ->
                    FeatureChoice(group.label, state.expenseGroup == group.group) { onGroup(group.group) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("开销小类", color = Muted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                expenseCategories(state.expenseGroup).forEach { category ->
                    FeatureChoice(category.key, state.category == category.key) { onCategory(category.key) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("重复频率", color = Muted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceFrequency.entries.forEach { frequency ->
                    FeatureChoice(frequencyLabel(frequency), state.frequency == frequency) { onFrequency(frequency) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("下次日期", color = Muted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    state.nextDueDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.SIMPLIFIED_CHINESE)),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.note, onValueChange = onNote, modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") }, maxLines = 2, shape = RoundedCornerShape(16.dp),
            )
            state.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) {
                Text("保存周期账目")
            }
        }
    }
    if (showDatePicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = state.nextDueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(picker) }
    }
}

@Composable
fun BudgetSettingsSheet(
    settings: BudgetSettings,
    billingCloseDay: Int,
    onDismiss: () -> Unit,
    onSave: (BudgetSettings) -> Unit,
) {
    var total by remember(settings) { mutableStateOf(settings.totalMinor.asInput()) }
    var fixed by remember(settings) { mutableStateOf(settings.fixedMinor.asInput()) }
    var subscription by remember(settings) { mutableStateOf(settings.subscriptionMinor.asInput()) }
    var investmentRatio by remember(settings) {
        val flexible = settings.investmentMinor + settings.dailyMinor
        mutableStateOf(if (flexible > 0) settings.investmentMinor.toFloat() / flexible else .3f)
    }
    val totalMinor = total.amountOrZero()
    val fixedMinor = fixed.amountOrZero()
    val subscriptionMinor = subscription.amountOrZero()
    val reserved = fixedMinor + subscriptionMinor
    val valid = totalMinor > 0 && reserved <= totalMinor
    val allocation = linkedBudgetSettings(totalMinor, fixedMinor, subscriptionMinor, investmentRatio)
    val cycle = billingCycleFor(LocalDate.now(), billingCloseDay)
    val weeklyAllowance = weeklyFlexibleAllowance(allocation, cycle.start, cycle.endInclusive)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Paper) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text("分配账单周期预算", style = MaterialTheme.typography.headlineMedium)
            Text("先预留固定支出，再分配投资与自由消费", color = Muted)
            Spacer(Modifier.height(18.dp))
            BudgetInput("总预算", total) { total = it }
            Spacer(Modifier.height(10.dp))
            BudgetInput("固定开销", fixed) { fixed = it }
            Spacer(Modifier.height(10.dp))
            BudgetInput("固定订阅", subscription) { subscription = it }
            Spacer(Modifier.height(18.dp))
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("预留后可分配", color = Muted, style = MaterialTheme.typography.labelLarge)
                    Text(formatMoney((totalMinor - reserved).coerceAtLeast(0), CurrencyCode.EUR), style = MaterialTheme.typography.headlineSmall)
                }
            }
            if (reserved > totalMinor && totalMinor > 0) {
                Spacer(Modifier.height(8.dp))
                Text("固定开销与订阅不能超过总预算", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("投资 ${investmentRatio.percentText()}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("自由消费 ${(1f - investmentRatio).percentText()}", style = MaterialTheme.typography.titleMedium)
            }
            Slider(
                value = investmentRatio,
                onValueChange = { investmentRatio = ((it * 20).roundToInt() / 20f).coerceIn(0f, 1f) },
                valueRange = 0f..1f,
                steps = 19,
                enabled = valid && allocation.totalMinor > reserved,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AllocationAmount("投资额度", allocation.investmentMinor, Modifier.weight(1f))
                AllocationAmount("自由消费", allocation.dailyMinor, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Surface(color = androidx.compose.ui.graphics.Color(0xFFDCECE6), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("自动生成的每周自由消费额度", color = Forest, style = MaterialTheme.typography.labelLarge)
                    Text(formatMoney(weeklyAllowance, CurrencyCode.EUR), color = Forest, style = MaterialTheme.typography.headlineSmall)
                    Text("按当前账单周期天数折算", color = Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    onSave(allocation)
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp),
            ) { Text("保存预算") }
        }
    }
}

@Composable
private fun AllocationAmount(label: String, amount: Long, modifier: Modifier) {
    Surface(modifier = modifier, color = Paper, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(formatMoney(amount, CurrencyCode.EUR), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BudgetInput(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(14)) },
        modifier = Modifier.fillMaxWidth(), label = { Text(label) }, leadingIcon = { Text("€") },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun FeatureChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) Forest else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) androidx.compose.ui.graphics.Color.White else Muted,
        shape = RoundedCornerShape(50),
    ) { Text(text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) }
}

fun frequencyLabel(frequency: RecurrenceFrequency): String = when (frequency) {
    RecurrenceFrequency.WEEKLY -> "每周"
    RecurrenceFrequency.MONTHLY -> "每月"
    RecurrenceFrequency.YEARLY -> "每年"
}

private fun Long.asInput(): String = if (this <= 0) "" else minorToInput(this)
private fun String.amountOrZero(): Long = parseMinor(this)?.coerceAtLeast(0) ?: 0
private fun Float.percentText(): String = "${(this * 100).roundToInt()}%"
