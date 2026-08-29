package com.dubiao.yibi.domain

import com.dubiao.yibi.data.CurrencyCode
import java.math.BigDecimal
import java.math.RoundingMode

private val hundred = BigDecimal(100)

fun parseMinor(text: String): Long? = runCatching {
    text.trim()
        .replace(',', '.')
        .takeIf { it.isNotBlank() }
        ?.let(::BigDecimal)
        ?.setScale(2, RoundingMode.HALF_UP)
        ?.multiply(hundred)
        ?.longValueExact()
}.getOrNull()

fun minorToInput(minor: Long): String = BigDecimal(minor)
    .divide(hundred, 2, RoundingMode.UNNECESSARY)
    .stripTrailingZeros()
    .toPlainString()

fun formatMoney(minor: Long, currency: CurrencyCode, signed: Boolean = false): String {
    val sign = when {
        minor < 0 -> "−"
        signed && minor > 0 -> "+"
        else -> ""
    }
    val symbol = when (currency) {
        CurrencyCode.EUR -> "€"
    }
    val value = BigDecimal(kotlin.math.abs(minor))
        .divide(hundred, 2, RoundingMode.UNNECESSARY)
        .toPlainString()
    val parts = value.split('.')
    val grouped = parts[0].reversed().chunked(3).joinToString(",").reversed()
    return "$sign$symbol$grouped.${parts.getOrElse(1) { "00" }}"
}
