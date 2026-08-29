@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dubiao.yibi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dubiao.yibi.ui.theme.Apricot
import com.dubiao.yibi.ui.theme.Coral
import com.dubiao.yibi.ui.theme.Forest
import com.dubiao.yibi.ui.theme.Hairline
import com.dubiao.yibi.ui.theme.Mint
import com.dubiao.yibi.ui.theme.Muted
import com.dubiao.yibi.ui.theme.Paper

@Composable
fun AddMethodSheet(
    isListening: Boolean,
    onDismiss: () -> Unit,
    onManual: () -> Unit,
    onVoice: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        dragHandle = {
            Surface(
                color = Hairline,
                shape = CircleShape,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp).size(38.dp, 4.dp),
            ) {}
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 30.dp),
        ) {
            Text("添加账目", style = MaterialTheme.typography.headlineMedium)
            Text("选择一种录入方式", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AddMethodCard(
                    title = "手动记账",
                    subtitle = "逐项填写金额与分类",
                    icon = Icons.Outlined.Edit,
                    containerColor = Mint,
                    contentColor = Forest,
                    onClick = onManual,
                    modifier = Modifier.weight(1f),
                )
                AddMethodCard(
                    title = if (isListening) "结束语音输入" else "语音记账",
                    subtitle = if (isListening) "轻触后进入确认页面" else "说出欧元金额、用途和日期",
                    icon = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    containerColor = if (isListening) Color(0xFFF7DED8) else Apricot.copy(alpha = .62f),
                    contentColor = if (isListening) Coral else Color(0xFF704817),
                    onClick = onVoice,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AddMethodCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(17.dp)) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(color = Paper.copy(alpha = .7f), shape = CircleShape) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = contentColor.copy(alpha = .72f),
                style = MaterialTheme.typography.labelMedium,
                minLines = 2,
            )
        }
    }
}
