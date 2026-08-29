package com.dubiao.yibi.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dubiao.yibi.data.ExpenseGroup

data class CategoryUi(
    val key: String,
    val icon: ImageVector,
    val color: Color,
    val tint: Color,
)

val categories = listOf(
    // 日常消费
    CategoryUi("餐饮", Icons.Outlined.Restaurant, Color(0xFFFFE6C9), Color(0xFF9A5915)),
    CategoryUi("交通", Icons.Outlined.DirectionsBus, Color(0xFFDCEBFA), Color(0xFF2E638F)),
    CategoryUi("购物", Icons.Outlined.ShoppingBag, Color(0xFFF2E0EC), Color(0xFF87506F)),
    CategoryUi("娱乐", Icons.Outlined.Theaters, Color(0xFFEAE2FA), Color(0xFF69528E)),
    CategoryUi("健康", Icons.Outlined.HealthAndSafety, Color(0xFFF9DEDA), Color(0xFF9A4E43)),
    CategoryUi("旅行", Icons.Outlined.FlightTakeoff, Color(0xFFD8EEEA), Color(0xFF39756A)),
    CategoryUi("礼物", Icons.Outlined.CardGiftcard, Color(0xFFFCE6D7), Color(0xFF9A5B34)),
    CategoryUi("其他日常", Icons.Outlined.MoreHoriz, Color(0xFFE8E8E3), Color(0xFF646C68)),

    // 固定开销
    CategoryUi("房租", Icons.Outlined.Home, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    CategoryUi("水电燃气", Icons.Outlined.Home, Color(0xFFDDEADF), Color(0xFF3F7049)),
    CategoryUi("保险", Icons.Outlined.HealthAndSafety, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    CategoryUi("通讯网络", Icons.Outlined.Wifi, Color(0xFFDDEADF), Color(0xFF3F7049)),
    CategoryUi("物业", Icons.Outlined.Home, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    CategoryUi("其他固定", Icons.Outlined.MoreHoriz, Color(0xFFDDEADF), Color(0xFF3F7049)),

    // 订阅开销
    CategoryUi("影音订阅", Icons.Outlined.Theaters, Color(0xFFEAE2FA), Color(0xFF69528E)),
    CategoryUi("软件服务", Icons.Outlined.Subscriptions, Color(0xFFF2E0EC), Color(0xFF87506F)),
    CategoryUi("云服务", Icons.Outlined.Subscriptions, Color(0xFFEAE2FA), Color(0xFF69528E)),
    CategoryUi("会员订阅", Icons.Outlined.CardGiftcard, Color(0xFFF2E0EC), Color(0xFF87506F)),
    CategoryUi("健身会员", Icons.Outlined.HealthAndSafety, Color(0xFFEAE2FA), Color(0xFF69528E)),
    CategoryUi("其他订阅", Icons.Outlined.MoreHoriz, Color(0xFFF2E0EC), Color(0xFF87506F)),

    // 投资花费
    CategoryUi("股票", Icons.Outlined.Insights, Color(0xFFDCEBFA), Color(0xFF2E638F)),
    CategoryUi("基金", Icons.Outlined.Insights, Color(0xFFDCEBFA), Color(0xFF2E638F)),
    CategoryUi("定投", Icons.Outlined.Payments, Color(0xFFDCEBFA), Color(0xFF2E638F)),
    CategoryUi("债券", Icons.Outlined.Payments, Color(0xFFDCEBFA), Color(0xFF2E638F)),
    CategoryUi("其他投资", Icons.Outlined.MoreHoriz, Color(0xFFDCEBFA), Color(0xFF2E638F)),

    // 收入
    CategoryUi("工资", Icons.Outlined.Payments, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    CategoryUi("报销", Icons.Outlined.Payments, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    CategoryUi("奖金", Icons.Outlined.Payments, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    CategoryUi("退款", Icons.Outlined.Payments, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    CategoryUi("礼金", Icons.Outlined.CardGiftcard, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    CategoryUi("其他收入", Icons.Outlined.MoreHoriz, Color(0xFFD8E9DF), Color(0xFF235B4D)),

    // 兼容旧账目的显示名称
    CategoryUi("居住", Icons.Outlined.Home, Color(0xFFDDEADF), Color(0xFF3F7049)),
    CategoryUi("收入", Icons.Outlined.Payments, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    CategoryUi("其他", Icons.Outlined.MoreHoriz, Color(0xFFE8E8E3), Color(0xFF646C68)),
)

fun categoryUi(key: String): CategoryUi = categories.firstOrNull { it.key == key } ?: categories.last()

private val expenseCategoryKeys = mapOf(
    ExpenseGroup.FIXED to listOf("房租", "水电燃气", "保险", "通讯网络", "物业", "其他固定"),
    ExpenseGroup.SUBSCRIPTION to listOf("影音订阅", "软件服务", "云服务", "会员订阅", "健身会员", "其他订阅"),
    ExpenseGroup.DAILY to listOf("餐饮", "交通", "购物", "娱乐", "健康", "旅行", "礼物", "其他日常"),
    ExpenseGroup.INVESTMENT to listOf("股票", "基金", "定投", "债券", "其他投资"),
)

private val incomeCategoryKeys = listOf("工资", "报销", "奖金", "退款", "礼金", "其他收入")

fun expenseCategories(group: ExpenseGroup): List<CategoryUi> = expenseCategoryKeys.getValue(group).map(::categoryUi)
fun incomeCategories(): List<CategoryUi> = incomeCategoryKeys.map(::categoryUi)
fun defaultExpenseCategory(group: ExpenseGroup): String = expenseCategoryKeys.getValue(group).first()
fun categoryBelongsTo(group: ExpenseGroup, category: String): Boolean = category in expenseCategoryKeys.getValue(group)
fun isIncomeCategory(category: String): Boolean = category in incomeCategoryKeys

data class ExpenseGroupUi(
    val group: ExpenseGroup,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val tint: Color,
)

val expenseGroups = listOf(
    ExpenseGroupUi(ExpenseGroup.FIXED, "固定开销", "房租、水电与保险", Icons.Outlined.Home, Color(0xFFD8E9DF), Color(0xFF235B4D)),
    ExpenseGroupUi(ExpenseGroup.SUBSCRIPTION, "订阅开销", "影音、软件与会员", Icons.Outlined.Subscriptions, Color(0xFFEAE2FA), Color(0xFF69528E)),
    ExpenseGroupUi(ExpenseGroup.DAILY, "日常消费", "餐饮、交通与购物", Icons.Outlined.ShoppingBag, Color(0xFFFFE6C9), Color(0xFF9A5915)),
    ExpenseGroupUi(ExpenseGroup.INVESTMENT, "投资花费", "基金、股票与定投", Icons.Outlined.Insights, Color(0xFFDCEBFA), Color(0xFF2E638F)),
)

fun expenseGroupUi(group: ExpenseGroup): ExpenseGroupUi = expenseGroups.first { it.group == group }
