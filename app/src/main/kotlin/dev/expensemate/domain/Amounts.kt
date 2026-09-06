package dev.expensemate.domain

import java.math.BigDecimal
import java.math.RoundingMode

object Amounts {
    fun parseAmount(raw: String): BigDecimal? {
        return raw.trim().takeIf { it.isNotEmpty() }?.let {
            runCatching { BigDecimal(it) }.getOrNull()
        }?.takeIf { it > BigDecimal.ZERO && it.scale() <= AMOUNT_DECIMAL_PLACES }
            ?.setScale(AMOUNT_DECIMAL_PLACES, RoundingMode.UNNECESSARY)
    }

    fun canStoreAmount(amount: BigDecimal): Boolean {
        return amount.movePointRight(2) <= MAX_AMOUNT_CENTS
    }

    private const val AMOUNT_DECIMAL_PLACES = 2
    private val MAX_AMOUNT_CENTS = BigDecimal.valueOf(Long.MAX_VALUE)
}
