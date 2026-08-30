package com.dubiao.yibi.domain

import com.dubiao.yibi.data.ExpenseGroup
import com.dubiao.yibi.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class VoiceParserTest {
    private val now = LocalDateTime.of(2026, 8, 26, 16, 30)

    @Test fun parsesChineseYuanExpense() {
        val result = VoiceParser.parse("昨天午饭八十三块五", now)
        assertEquals("83.5", result.amountText)
        assertEquals("餐饮", result.category)
        assertEquals(TransactionType.EXPENSE, result.type)
        val expected = LocalDateTime.of(2026, 8, 25, 12, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result.occurredAt)
    }

    @Test fun parsesDollarIncome() {
        val result = VoiceParser.parse("收到报销25.50美元", now)
        assertEquals("25.50", result.amountText)
        assertEquals(TransactionType.INCOME, result.type)
    }

    @Test fun defaultsToEuro() {
        val result = VoiceParser.parse("咖啡四欧二", now)
        assertEquals("4.2", result.amountText)
        assertEquals("餐饮", result.category)
    }

    @Test fun standaloneYuanWordIsStillParsedAsEuroAmount() {
        val result = VoiceParser.parse("午饭83元", now)
        assertEquals("83", result.amountText)
    }

    @Test fun correctsBaToEightInClearAmountPositions() {
        assertEquals("8", VoiceParser.parse("午饭吧欧", now).amountText)
        assertEquals("6.8", VoiceParser.parse("咖啡六点吧欧", now).amountText)
        assertEquals("83", VoiceParser.parse("晚饭吧十三块", now).amountText)
    }

    @Test fun doesNotTreatBaInVenueNamesAsEight() {
        val bar = VoiceParser.parse("酒吧十三欧", now)
        assertEquals("13", bar.amountText)
        assertEquals("娱乐", bar.category)
    }

    @Test fun recognizesFixedExpenseGroup() {
        val result = VoiceParser.parse("房租八百欧元", now)
        assertEquals(ExpenseGroup.FIXED, result.expenseGroup)
        assertEquals("房租", result.category)
    }

    @Test fun recognizesSubscriptionExpenseGroup() {
        val result = VoiceParser.parse("Netflix订阅十八美元", now)
        assertEquals(ExpenseGroup.SUBSCRIPTION, result.expenseGroup)
        assertEquals("影音订阅", result.category)
    }

    @Test fun recognizesInvestmentExpenseGroup() {
        val result = VoiceParser.parse("ETF定投一百欧元", now)
        assertEquals(ExpenseGroup.INVESTMENT, result.expenseGroup)
        assertEquals("基金", result.category)
    }

    @Test fun parsesNaturalChineseDecimalBeforeEuro() {
        val result = VoiceParser.parse("午饭八十三点五欧", now)
        assertEquals("83.5", result.amountText)
    }

    @Test fun parsesChineseAmountWithoutCurrencyAsEuro() {
        val result = VoiceParser.parse("今天咖啡十二点六", now)
        assertEquals("12.6", result.amountText)
    }

    @Test fun parsesEuroBeforeChineseAmount() {
        val result = VoiceParser.parse("超市欧元一百零五点二五", now)
        assertEquals("105.25", result.amountText)
    }

    @Test fun mixedChineseAndEnglishPlacesPreserveTextAndParseFields() {
        data class Case(
            val text: String,
            val amount: String,
            val category: String,
        )
        val cases = listOf(
            Case("昨天在Berlin吃午饭23.5欧", "23.5", "餐饮"),
            Case("Paris Starbucks咖啡六点八欧", "6.8", "餐饮"),
            Case("从Frankfurt到Munich坐火车45欧", "45", "交通"),
            Case("Amsterdam hotel 120 euro", "120", "旅行"),
            Case("Uber London 18.40 EUR", "18.40", "交通"),
            Case("Oslo A&O Hostel四十二欧", "42", "旅行"),
            Case("New York餐厅一百零八欧五", "108.5", "餐饮"),
            Case("Brussels Midi买咖啡4,20欧", "4.20", "餐饮"),
            Case("在Köln的REWE买菜32欧", "32", "购物"),
            Case("Düsseldorf坐Bahn十六欧", "16", "交通"),
        )
        cases.forEach { case ->
            val result = VoiceParser.parse(case.text, now)
            assertEquals(case.text, result.rawText)
            assertEquals("amount for ${case.text}", case.amount, result.amountText)
            assertEquals("category for ${case.text}", case.category, result.category)
        }
    }

    @Test fun englishPlaceContainingEurIsNotMistakenForCurrencyToken() {
        val result = VoiceParser.parse("Eureka咖啡12.5", now)
        assertEquals("12.5", result.amountText)
        assertEquals("餐饮", result.category)
    }

    @Test fun chineseWordEuropeIsNotMistakenForEuroMarker() {
        val result = VoiceParser.parse("欧洲旅行花了50", now)
        assertEquals("50", result.amountText)
        assertEquals("旅行", result.category)
    }

    @Test fun currencyWordsNeverChangeTheEuroUnit() {
        val usd = VoiceParser.parse("Berlin hotel 80 dollars", now)
        val cny = VoiceParser.parse("Paris餐厅人民币120", now)
        assertEquals("80", usd.amountText)
        assertEquals("120", cny.amountText)
    }

    @Test fun usesLaterRecognitionCandidateWhenFirstHasNoAmount() {
        val result = VoiceParser.selectBestCandidate(
            listOf("昨天在 Berlin Starbucks 喝咖啡", "昨天在 Berlin Starbucks 喝咖啡六点八欧"),
        )
        assertEquals("昨天在 Berlin Starbucks 喝咖啡六点八欧", result)
    }

    @Test fun keepsTopRecognitionCandidateWhenItIsComplete() {
        val result = VoiceParser.selectBestCandidate(
            listOf("Düsseldorf坐Bahn十六欧", "Düsseldorf坐Bahn十欧"),
        )
        assertEquals("Düsseldorf坐Bahn十六欧", result)
    }
}
