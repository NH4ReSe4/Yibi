@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.dubiao.yibi.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.InputMethod
import com.dubiao.yibi.data.TransactionType
import com.dubiao.yibi.ui.theme.Coral
import com.dubiao.yibi.ui.theme.Forest
import com.dubiao.yibi.ui.theme.Hairline
import com.dubiao.yibi.ui.theme.Mint
import com.dubiao.yibi.ui.theme.Muted
import com.dubiao.yibi.ui.theme.Paper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionEditorSheet(
    state: EditorState,
    onDismiss: () -> Unit,
    onType: (TransactionType) -> Unit,
    onAmount: (String) -> Unit,
    onCategory: (String) -> Unit,
    onExpenseGroup: (ExpenseGroup) -> Unit,
    onDate: (LocalDate) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp).size(38.dp, 4.dp).background(Hairline, CircleShape))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (state.editingId == 0L) "添加账目" else "编辑账目", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        when (state.inputMethod) {
                            InputMethod.VOICE -> "语音内容已填入，请确认后保存"
                            InputMethod.RECURRING -> "由周期账目按到期日自动生成"
                            InputMethod.MANUAL -> "仅记录欧元金额"
                        },
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (state.editingId != 0L) {
                    TextButton(onClick = { confirmDelete = true }, colors = ButtonDefaults.textButtonColors(contentColor = Coral)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除")
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            TypeSelector(state.type, onType)
            Spacer(Modifier.height(18.dp))

            Text("金额（欧元）", color = Muted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("€", style = MaterialTheme.typography.displaySmall, color = Forest)
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = onAmount,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    placeholder = { Text("0.00", style = MaterialTheme.typography.headlineLarge) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(18.dp),
                )
            }

            if (state.type == TransactionType.EXPENSE) {
                Spacer(Modifier.height(22.dp))
                Text("开销大类", color = Muted, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    expenseGroups.forEach { group ->
                        ChoicePill(
                            text = group.label,
                            selected = state.expenseGroup == group.group,
                            onClick = { onExpenseGroup(group.group) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(if (state.type == TransactionType.EXPENSE) "开销小类" else "收入类别", color = Muted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                val availableCategories = if (state.type == TransactionType.EXPENSE) {
                    expenseCategories(state.expenseGroup)
                } else {
                    incomeCategories()
                }
                availableCategories.forEach { category ->
                    CategoryChoice(category, state.category == category.key) { onCategory(category.key) }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("日期", color = Muted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(9.dp))
            val selectedDate = Instant.ofEpochMilli(state.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = Forest,
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(color = Mint, shape = CircleShape) {
                        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(21.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.SIMPLIFIED_CHINESE)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(dateDescription(selectedDate), color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "打开日历", tint = Muted)
                }
            }

            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = onNote,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                placeholder = { Text("例如：午餐、房租、Netflix") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(18.dp),
            )

            if (state.inputMethod == InputMethod.VOICE && !state.rawVoiceText.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("语音原文：${state.rawVoiceText}", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
            ) {
                Text(if (state.editingId == 0L) "保存账目" else "保存修改", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showDatePicker) {
        val selectedDate = Instant.ofEpochMilli(state.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            onDate(Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("选择日期", modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
                headline = null,
                showModeToggle = false,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("确定删除这笔账目吗？") },
            text = { Text("删除后将无法恢复。") },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Coral),
                ) { Text("删除") }
            },
        )
    }
}

private fun dateDescription(date: LocalDate): String {
    val relative = when (date) {
        LocalDate.now() -> "今天"
        LocalDate.now().minusDays(1) -> "昨天"
        else -> null
    }
    val weekday = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.SIMPLIFIED_CHINESE))
    return listOfNotNull(relative, weekday).joinToString(" · ")
}

@Composable
private fun TypeSelector(selected: TransactionType, onType: (TransactionType) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(4.dp)) {
            listOf(TransactionType.EXPENSE to "支出", TransactionType.INCOME to "收入").forEach { (type, label) ->
                Surface(
                    modifier = Modifier.weight(1f).clickable { onType(type) },
                    color = if (selected == type) Paper else Color.Transparent,
                    shape = RoundedCornerShape(13.dp),
                    shadowElevation = if (selected == type) 1.dp else 0.dp,
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 11.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (selected == type) Forest else Muted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) Forest else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.White else Muted,
        shape = CircleShape,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CategoryChoice(category: CategoryUi, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) Forest else category.color,
        contentColor = if (selected) Color.White else category.tint,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(category.icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(category.key, style = MaterialTheme.typography.labelLarge)
        }
    }
}
