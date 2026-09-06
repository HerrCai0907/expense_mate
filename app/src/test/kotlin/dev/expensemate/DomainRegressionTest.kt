package dev.expensemate

import ExpenseAnalysisTest
import dev.expensemate.domain.Amounts
import dev.expensemate.domain.ExpenseRecords
import dev.expensemate.domain.TagPriorityRanker
import dev.expensemate.domain.parsePresetTime
import dev.expensemate.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DomainRegressionTest {
    @Test fun existingAnalysisRegressions() {
        val originalZone = java.util.TimeZone.getDefault()
        try {
            ExpenseAnalysisTest.main(emptyArray())
        } finally {
            java.util.TimeZone.setDefault(originalZone)
        }
    }

    @Test fun amountValidationPreservesPrecisionAndStorageBoundary() {
        assertEquals(BigDecimal("12.30"), Amounts.parseAmount(" 12.3 "))
        listOf("", "abc", "0", "-1", "1.001").forEach { assertNull(Amounts.parseAmount(it)) }
        val max = Amounts.parseAmount("92233720368547758.07")!!
        assertTrue(Amounts.canStoreAmount(max))
        assertFalse(Amounts.canStoreAmount(max + BigDecimal("0.01")))
    }

    @Test fun tagRankingNormalizesAndKeepsRecentTagsFirst() {
        val ranked = TagPriorityRanker().rank(
            listOf(" 餐饮 ", "交通", "餐饮", "", "购物"),
            listOf("购物", "unknown", "购物", " 餐饮 ")
        )
        assertEquals(listOf("购物", "餐饮", "交通"), ranked.map { it.tag })
        assertEquals(listOf(100, 80, 70), ranked.map { it.priority })
        assertEquals(0, TagPriorityRanker().rank((1..11).map { "$it" }, emptyList()).last().priority)
    }

    @Test fun recordSortsHaveDeterministicTieBreaks() {
        val records = listOf(
            Expense(1, 100, emptyList(), "", 1000),
            Expense(2, 200, emptyList(), "", 2000),
            Expense(3, 100, emptyList(), "", 2000),
            Expense(4, 100, emptyList(), "", 2000)
        )
        fun ids(mode: Int) = ExpenseRecords.sorted(records, mode).map { it.id }
        assertEquals(listOf(4L, 3L, 2L, 1L), ids(0))
        assertEquals(listOf(1L, 4L, 3L, 2L), ids(1))
        assertEquals(listOf(2L, 4L, 3L, 1L), ids(2))
        assertEquals(listOf(4L, 3L, 1L, 2L), ids(3))
        assertEquals(listOf(1L, 2L, 3L, 4L), records.map { it.id })
    }

    @Test fun presetTimeValidationPreservesBoundaries() {
        assertEquals(0 to 0, parsePresetTime("00:00"))
        assertEquals(8 to 30, parsePresetTime("8:30"))
        assertEquals(23 to 59, parsePresetTime("23:59"))
        listOf("24:00", "12:60", "8:3", "abc", "-1:00").forEach { assertNull(parsePresetTime(it)) }
    }
}
