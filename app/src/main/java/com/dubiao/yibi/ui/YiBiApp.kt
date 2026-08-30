package com.dubiao.yibi.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dubiao.yibi.domain.recurringBudgetReserve
import com.dubiao.yibi.speech.OnlineSpeechController
import com.dubiao.yibi.ui.theme.Forest
import com.dubiao.yibi.ui.theme.Mint
import com.dubiao.yibi.ui.theme.Muted
import com.dubiao.yibi.ui.theme.Paper
import com.dubiao.yibi.update.AppUpdateManager
import com.dubiao.yibi.update.AppUpdateState
import com.dubiao.yibi.update.InstallResult
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("首页", Icons.Outlined.Home),
    LEDGER("流水", Icons.AutoMirrored.Outlined.ReceiptLong),
    REPORTS("报表", Icons.Outlined.Analytics),
    SETTINGS("设置", Icons.Outlined.Settings),
}

@Composable
fun YiBiApp(viewModel: LedgerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val billingCloseDay by viewModel.billingCloseDay.collectAsStateWithLifecycle()
    val budgetSettings by viewModel.budgetSettings.collectAsStateWithLifecycle()
    val weeklyReminderEnabled by viewModel.weeklyReminderEnabled.collectAsStateWithLifecycle()
    val recurringTemplates by viewModel.recurringTemplates.collectAsStateWithLifecycle()
    val recurringReserve = remember(recurringTemplates) { recurringBudgetReserve(recurringTemplates) }
    val recurringEditor by viewModel.recurringEditor.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var isListening by remember { mutableStateOf(false) }
    var showAddMethod by remember { mutableStateOf(false) }
    var showBudgetSettings by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val updateManager = remember(context) { AppUpdateManager(context.applicationContext) }
    var updateState by remember { mutableStateOf<AppUpdateState>(AppUpdateState.Idle) }

    val checkForUpdate: () -> Unit = {
        scope.launch {
            updateState = AppUpdateState.Checking
            try {
                val update = updateManager.check()
                updateState = update?.let { AppUpdateState.Available(it) } ?: AppUpdateState.UpToDate
            } catch (error: Throwable) {
                updateState = AppUpdateState.Failed(error.message ?: "暂时无法检查更新")
            }
        }
        Unit
    }
    val downloadUpdate: () -> Unit = {
        val info = (updateState as? AppUpdateState.Available)?.info
        if (info != null) {
            scope.launch {
                updateState = AppUpdateState.Downloading(info, null)
                try {
                    val apk = updateManager.download(info) { percent ->
                        updateState = AppUpdateState.Downloading(info, percent)
                    }
                    updateState = AppUpdateState.Ready(info, apk)
                } catch (error: Throwable) {
                    updateState = AppUpdateState.Failed(error.message ?: "更新下载失败")
                }
            }
        }
        Unit
    }
    val installUpdate: () -> Unit = {
        val ready = updateState as? AppUpdateState.Ready
        if (ready != null) {
            when (updateManager.install(ready.apk)) {
                InstallResult.STARTED -> Unit
                InstallResult.PERMISSION_REQUIRED -> scope.launch {
                    snackbar.showSnackbar("请允许一笔安装未知应用，返回后再次点击安装")
                }
            }
        }
        Unit
    }

    val showTransferResult: (Boolean, String) -> Unit = { success, message ->
        scope.launch { snackbar.showSnackbar(if (success) message else "操作失败：$message") }
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportBackup(context, it, showTransferResult) } }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let { viewModel.exportCsv(context, it, showTransferResult) } }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.restoreBackup(context, it, showTransferResult) } }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted = granted
        if (!granted) scope.launch { snackbar.showSnackbar("未允许通知，每周提醒暂时无法显示") }
    }
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(weeklyReminderEnabled) {
        if (weeklyReminderEnabled) requestNotificationPermission()
    }

    val speechController = remember(context) {
        OnlineSpeechController(
            context = context.applicationContext,
            onListeningChanged = { isListening = it },
            onResult = viewModel::openVoiceResult,
            onError = { message -> scope.launch { snackbar.showSnackbar(message) } },
        )
    }
    DisposableEffect(speechController) {
        onDispose { speechController.destroy() }
    }

    var requestVoiceAfterPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && requestVoiceAfterPermission) speechController.start()
        else if (!granted) scope.launch { snackbar.showSnackbar("麦克风权限未开启，仍可使用手动记账") }
        requestVoiceAfterPermission = false
    }

    val startVoice = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            speechController.start()
        } else {
            requestVoiceAfterPermission = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val toggleVoice = {
        if (isListening) speechController.stop() else startVoice()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (selectedTab != AppTab.SETTINGS) {
                FloatingActionButton(
                    onClick = { showAddMethod = true },
                    containerColor = Forest,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "添加账目")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Paper) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Forest,
                            selectedTextColor = Forest,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                            indicatorColor = Mint,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            AppTab.HOME -> HomeScreen(
                transactions = transactions,
                billingCloseDay = billingCloseDay,
                budgetSettings = budgetSettings,
                isListening = isListening,
                onManual = viewModel::openManual,
                onVoice = toggleVoice,
                onSeeAll = { selectedTab = AppTab.LEDGER },
                onTransaction = viewModel::edit,
                modifier = Modifier.padding(padding),
            )
            AppTab.LEDGER -> LedgerScreen(
                transactions = transactions,
                onAdd = viewModel::openManual,
                onTransaction = viewModel::edit,
                modifier = Modifier.padding(padding),
            )
            AppTab.REPORTS -> ReportsScreen(
                transactions = transactions,
                modifier = Modifier.padding(padding),
            )
            AppTab.SETTINGS -> SettingsScreen(
                billingCloseDay = billingCloseDay,
                budgetSettings = budgetSettings,
                recurringReserve = recurringReserve,
                recurringTemplates = recurringTemplates,
                onBillingCloseDay = { day ->
                    viewModel.setBillingCloseDay(day)
                    scope.launch { snackbar.showSnackbar("账单截止日已设置为每月 ${day} 日") }
                },
                onEditBudget = { showBudgetSettings = true },
                onAddRecurring = viewModel::openRecurringTemplate,
                onEditRecurring = viewModel::editRecurringTemplate,
                onDeleteRecurring = { template ->
                    viewModel.deleteRecurringTemplate(template) {
                        scope.launch { snackbar.showSnackbar("周期账目已删除") }
                    }
                },
                weeklyReminderEnabled = weeklyReminderEnabled,
                notificationPermissionGranted = notificationPermissionGranted,
                onWeeklyReminderEnabled = { enabled ->
                    viewModel.setWeeklyReminderEnabled(context.applicationContext, enabled)
                    if (enabled) requestNotificationPermission()
                },
                onRequestNotificationPermission = requestNotificationPermission,
                onExportBackup = { backupLauncher.launch("一笔完整备份-${LocalDate.now()}.json") },
                onRestoreBackup = { confirmRestore = true },
                onExportCsv = { csvLauncher.launch("一笔流水-${LocalDate.now()}.csv") },
                updateState = updateState,
                onCheckUpdate = checkForUpdate,
                onDownloadUpdate = downloadUpdate,
                onInstallUpdate = installUpdate,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showAddMethod) {
        AddMethodSheet(
            isListening = isListening,
            onDismiss = { showAddMethod = false },
            onManual = {
                showAddMethod = false
                viewModel.openManual()
            },
            onVoice = {
                showAddMethod = false
                toggleVoice()
            },
        )
    }

    if (editor.visible) {
        TransactionEditorSheet(
            state = editor,
            onDismiss = viewModel::dismissEditor,
            onType = viewModel::setType,
            onAmount = viewModel::setAmount,
            onCategory = viewModel::setCategory,
            onExpenseGroup = viewModel::setExpenseGroup,
            onDate = viewModel::setDate,
            onNote = viewModel::setNote,
            onSave = { viewModel.saveEditor { scope.launch { snackbar.showSnackbar("账目已保存") } } },
            onDelete = { viewModel.deleteEditing { scope.launch { snackbar.showSnackbar("账目已删除") } } },
        )
    }

    if (recurringEditor.visible) {
        RecurringTemplateSheet(
            state = recurringEditor,
            onDismiss = viewModel::dismissRecurringEditor,
            onName = viewModel::setRecurringName,
            onAmount = viewModel::setRecurringAmount,
            onGroup = viewModel::setRecurringGroup,
            onCategory = viewModel::setRecurringCategory,
            onFrequency = viewModel::setRecurringFrequency,
            onDate = viewModel::setRecurringDate,
            onNote = viewModel::setRecurringNote,
            onSave = { viewModel.saveRecurringTemplate { scope.launch { snackbar.showSnackbar("周期账目已保存") } } },
        )
    }

    if (showBudgetSettings) {
        BudgetSettingsSheet(
            settings = budgetSettings,
            billingCloseDay = billingCloseDay,
            recurringReserve = recurringReserve,
            onDismiss = { showBudgetSettings = false },
            onSave = { settings ->
                viewModel.setBudgetSettings(settings)
                showBudgetSettings = false
                scope.launch { snackbar.showSnackbar("预算已保存") }
            },
        )
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("恢复完整备份？") },
            text = { Text("恢复会用备份文件覆盖当前账目、周期模板、预算和账单截止日。建议先导出一份当前备份。") },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("取消") } },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    restoreLauncher.launch(arrayOf("application/json", "text/plain"))
                }) { Text("选择备份文件") }
            },
        )
    }
}
