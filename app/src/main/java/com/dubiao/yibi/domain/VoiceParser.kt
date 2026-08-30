package com.dubiao.yibi.domain

import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class VoiceDraft(
    val type: TransactionType,
    val amountText: String?,
    val category: String,
    val expenseGroup: ExpenseGroup,
    val occurredAt: Long,
    val note: String,
    val rawText: String,
)

object VoiceParser {
    private val incomeWords = listOf("收入", "工资", "薪水", "到账", "收到", "报销", "奖金", "退款")
    private val expenseCategoryRules = linkedMapOf(
        "房租" to listOf("房租", "租金"),
        "水电燃气" to listOf("水费", "电费", "燃气", "暖气"),
        "保险" to listOf("保险", "保费"),
        "通讯网络" to listOf("网费", "宽带", "话费", "手机套餐"),
        "物业" to listOf("物业"),
        "影音订阅" to listOf("netflix", "spotify", "视频会员", "音乐订阅"),
        "软件服务" to listOf("软件订阅", "应用订阅", "office", "adobe"),
        "云服务" to listOf("icloud", "云服务", "云盘"),
        "健身会员" to listOf("健身会员", "健身月费", "健身年费"),
        "会员订阅" to listOf("会员", "订阅", "月费", "年费"),
        "股票" to listOf("股票", "买股"),
        "基金" to listOf("基金", "etf"),
        "定投" to listOf("定投"),
        "债券" to listOf("债券"),
        "餐饮" to listOf("吃饭", "午饭", "晚饭", "早餐", "咖啡", "奶茶", "餐厅", "外卖", "麦当劳", "瑞幸", "starbucks", "restaurant", "cafe", "café", "coffee"),
        "交通" to listOf("地铁", "公交", "打车", "出租", "火车", "机票", "加油", "停车", "uber", "train", "bahn", "flixtrain", "taxi"),
        "购物" to listOf("买", "购物", "超市", "亚马逊", "衣服", "书", "rewe", "lidl", "aldi", "edeka", "amazon"),
        "娱乐" to listOf("电影", "游戏", "演出", "音乐", "酒吧"),
        "健康" to listOf("医院", "看病", "药", "牙医", "健身"),
        "旅行" to listOf("酒店", "旅行", "景点", "民宿", "hotel", "hostel", "airbnb"),
    )

    fun selectBestCandidate(candidates: List<String>): String? {
        val usable = candidates.map(String::trim).filter(String::isNotEmpty)
        val first = usable.firstOrNull() ?: return null
        if (parse(first).amountText != null) return first
        return usable.drop(1).firstOrNull { parse(it).amountText != null } ?: first
    }

    fun parse(text: String, now: LocalDateTime = LocalDateTime.now()): VoiceDraft {
        val normalized = text.trim().lowercase()
        val amount = detectAmount(correctAmountHomophones(normalized))
        val type = if (incomeWords.any(normalized::contains)) TransactionType.INCOME else TransactionType.EXPENSE
        val category = if (type == TransactionType.INCOME) {
            detectIncomeCategory(normalized)
        } else {
            expenseCategoryRules.entries.firstOrNull { (_, words) -> words.any(normalized::contains) }?.key
                ?: defaultCategoryFor(detectExpenseGroup(normalized, type))
        }
        return VoiceDraft(
            type = type,
            amountText = amount,
            category = category,
            expenseGroup = detectExpenseGroup(normalized, type),
            occurredAt = detectDateTime(normalized, now)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            note = text.trim(),
            rawText = text.trim(),
        )
    }

    private fun correctAmountHomophones(text: String): String {
        // 只在“吧”后面紧跟数字和币种时纠正，避免影响“酒吧”和“网吧”等正常词语。
        val currency = "(?:欧元|€|欧|块|元|\\b(?:eur|euros?)\\b)"
        val followingNumber = "[零〇一二两三四五六七八九十百千万点0-9]{0,8}"
        return text.replace(Regex("(?<![酒网])吧(?=$followingNumber\\s*$currency)"), "八")
    }

    private fun detectIncomeCategory(text: String): String = when {
        listOf("工资", "薪水", "薪资").any(text::contains) -> "工资"
        "报销" in text -> "报销"
        "奖金" in text -> "奖金"
        listOf("退款", "退钱").any(text::contains) -> "退款"
        listOf("礼金", "红包", "礼物").any(text::contains) -> "礼金"
        else -> "其他收入"
    }

    private fun defaultCategoryFor(group: ExpenseGroup): String = when (group) {
        ExpenseGroup.FIXED -> "其他固定"
        ExpenseGroup.SUBSCRIPTION -> "其他订阅"
        ExpenseGroup.DAILY -> "其他日常"
        ExpenseGroup.INVESTMENT -> "其他投资"
    }

    private fun detectExpenseGroup(text: String, type: TransactionType): ExpenseGroup {
        if (type == TransactionType.INCOME) return ExpenseGroup.DAILY
        return when {
            listOf("投资", "股票", "基金", "定投", "债券").any(text::contains) || Regex("\\betf\\b").containsMatchIn(text) -> ExpenseGroup.INVESTMENT
            listOf("订阅", "会员", "月费", "年费", "netflix", "spotify", "icloud").any(text::contains) -> ExpenseGroup.SUBSCRIPTION
            listOf("房租", "水费", "电费", "燃气", "物业", "保险", "宽带").any(text::contains) -> ExpenseGroup.FIXED
            else -> ExpenseGroup.DAILY
        }
    }

    private fun detectAmount(text: String): String? {
        // “块/元”只作为中文口语中的金额分隔词；所有数值仍统一按欧元存储。
        val tokenPattern = "(?:欧元|€|欧|块|元|\\b(?:eur|euros?)\\b)"
        Regex("(\\d+(?:[.,]\\d{1,2})?)\\s*$tokenPattern(?:\\s*([0-9一二两三四五六七八九]))?")
            .find(text)
            ?.let { match ->
                val whole = match.groupValues[1].replace(',', '.')
                val tail = match.groupValues[2]
                return if (tail.isNotEmpty() && !whole.contains('.')) {
                    "$whole.${digitValue(tail.first())}"
                } else whole
            }

        Regex("$tokenPattern\\s*(\\d+(?:[.,]\\d{1,2})?)")
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.let { return it.replace(',', '.') }

        val token = Regex(tokenPattern).findAll(text).firstOrNull { match ->
            hasAmountNextTo(text, match.range.first, match.range.last)
        }
        if (token == null) {
            Regex("\\d+(?:[.,]\\d{1,2})?").findAll(text).lastOrNull()?.value?.let {
                return it.replace(',', '.')
            }
            return chineseAmountRegex.findAll(text).lastOrNull()?.value?.let(::parseChineseAmount)
        }

        val prefix = text.substring(0, token.range.first)
            .takeLast(16)
            .trimEnd()
            .takeLastWhile { it in chineseAmountChars }
        val suffix = text.substring(token.range.last + 1)
            .trimStart()
            .takeWhile { it in chineseAmountChars }
            .take(16)
        val prefixAmount = parseChineseAmount(prefix)
        if (prefixAmount != null) {
            val decimalTail = suffix.takeIf { it.isNotBlank() && it.length <= 2 && it.all(chineseDigits::contains) }
            return if (decimalTail != null && !prefix.contains('点')) {
                "$prefixAmount.${decimalTail.map(::digitValue).joinToString("")}"
            } else prefixAmount
        }
        return parseChineseAmount(suffix)
    }

    private fun detectDateTime(text: String, now: LocalDateTime): LocalDateTime {
        var date = when {
            "前天" in text -> now.toLocalDate().minusDays(2)
            "昨天" in text -> now.toLocalDate().minusDays(1)
            "上周" in text -> {
                val day = chineseWeekday(text) ?: now.dayOfWeek
                now.toLocalDate().minusWeeks(1).with(TemporalAdjusters.previousOrSame(day))
            }
            else -> now.toLocalDate()
        }
        val explicitDate = Regex("(\\d{1,2})月(\\d{1,2})[日号]?").find(text)
        if (explicitDate != null) {
            date = runCatching {
                LocalDate.of(now.year, explicitDate.groupValues[1].toInt(), explicitDate.groupValues[2].toInt())
            }.getOrDefault(date)
        }
        val time = when {
            "早上" in text || "早餐" in text -> LocalTime.of(8, 0)
            "中午" in text || "午饭" in text -> LocalTime.of(12, 0)
            "下午" in text -> LocalTime.of(15, 0)
            "晚上" in text || "晚饭" in text -> LocalTime.of(19, 0)
            else -> now.toLocalTime().withSecond(0).withNano(0)
        }
        return LocalDateTime.of(date, time)
    }

    private fun chineseWeekday(text: String): DayOfWeek? = when {
        "一" in text.substringAfter("上周", "") -> DayOfWeek.MONDAY
        "二" in text.substringAfter("上周", "") -> DayOfWeek.TUESDAY
        "三" in text.substringAfter("上周", "") -> DayOfWeek.WEDNESDAY
        "四" in text.substringAfter("上周", "") -> DayOfWeek.THURSDAY
        "五" in text.substringAfter("上周", "") -> DayOfWeek.FRIDAY
        "六" in text.substringAfter("上周", "") -> DayOfWeek.SATURDAY
        "日" in text.substringAfter("上周", "") || "天" in text.substringAfter("上周", "") -> DayOfWeek.SUNDAY
        else -> null
    }

    private val chineseDigits = "零〇一二两三四五六七八九"
    private val chineseNumberChars = chineseDigits + "十百千万"
    private val chineseAmountChars = chineseNumberChars + "点"
    private val chineseAmountRegex = Regex("[$chineseNumberChars]+(?:点[$chineseDigits]{1,2})?")

    private fun hasAmountNextTo(text: String, start: Int, endInclusive: Int): Boolean {
        val before = text.substring(0, start).trimEnd().lastOrNull()
        val after = text.substring(endInclusive + 1).trimStart().firstOrNull()
        return before?.let { it.isDigit() || it in chineseAmountChars || it == '.' || it == ',' } == true ||
            after?.let { it.isDigit() || it in chineseAmountChars || it == '.' || it == ',' } == true
    }

    private fun digitValue(char: Char): Int = when (char) {
        '一' -> 1
        '二', '两' -> 2
        '三' -> 3
        '四' -> 4
        '五' -> 5
        '六' -> 6
        '七' -> 7
        '八' -> 8
        '九' -> 9
        else -> char.digitToIntOrNull() ?: 0
    }

    private fun parseChineseInteger(value: String): Long? {
        if (value.isBlank()) return null
        if (value.all { it in chineseDigits }) {
            return value.fold(0L) { total, char -> total * 10 + digitValue(char) }
        }
        var result = 0L
        var section = 0L
        var number = 0L
        value.forEach { char ->
            when (char) {
                in chineseDigits -> number = digitValue(char).toLong()
                '十' -> { section += (if (number == 0L) 1 else number) * 10; number = 0 }
                '百' -> { section += (if (number == 0L) 1 else number) * 100; number = 0 }
                '千' -> { section += (if (number == 0L) 1 else number) * 1_000; number = 0 }
                '万' -> { result += (section + number).coerceAtLeast(1) * 10_000; section = 0; number = 0 }
            }
        }
        return result + section + number
    }

    private fun parseChineseAmount(value: String): String? {
        if (value.isBlank()) return null
        val parts = value.split('点', limit = 2)
        val whole = parseChineseInteger(parts[0]) ?: return null
        if (parts.size == 1) return whole.toString()
        val decimals = parts[1].take(2)
        if (decimals.isBlank() || decimals.any { it !in chineseDigits }) return null
        return "$whole.${decimals.map(::digitValue).joinToString("")}"
    }
}
