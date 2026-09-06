package com.example.expensemate

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.math.BigDecimal

data class Expense(
    val id: Long,
    val amountCents: Long,
    val tags: List<String>,
    val detail: String,
    val createdAtMillis: Long
)

class ExpenseDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount_cents INTEGER NOT NULL,
                tags TEXT NOT NULL,
                detail TEXT NOT NULL,
                created_at_millis INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_expenses_created_at ON expenses(created_at_millis)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS expenses")
        onCreate(db)
    }

    fun insert(
        amount: BigDecimal,
        tags: List<String>,
        detail: String,
        createdAtMillis: Long = System.currentTimeMillis()
    ): Long {
        val values = ContentValues().apply {
            put("amount_cents", amount.movePointRight(2).setScale(0).longValueExact())
            put("tags", tags.joinToString(TAG_SEPARATOR))
            put("detail", detail)
            put("created_at_millis", createdAtMillis)
        }
        return writableDatabase.insert("expenses", null, values)
    }

    fun expensesSince(startMillis: Long): List<Expense> {
        val results = mutableListOf<Expense>()
        readableDatabase.query(
            "expenses",
            arrayOf("id", "amount_cents", "tags", "detail", "created_at_millis"),
            "created_at_millis >= ?",
            arrayOf(startMillis.toString()),
            null,
            null,
            "created_at_millis DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                results += Expense(
                    id = cursor.getLong(0),
                    amountCents = cursor.getLong(1),
                    tags = cursor.getString(2).split(TAG_SEPARATOR).filter { it.isNotBlank() },
                    detail = cursor.getString(3),
                    createdAtMillis = cursor.getLong(4)
                )
            }
        }
        return results
    }

    fun totalSince(startMillis: Long): Long {
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(amount_cents), 0) FROM expenses WHERE created_at_millis >= ?",
            arrayOf(startMillis.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    fun countSince(startMillis: Long): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM expenses WHERE created_at_millis >= ?",
            arrayOf(startMillis.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    companion object {
        private const val DB_NAME = "expense_mate.db"
        private const val DB_VERSION = 1
        private const val TAG_SEPARATOR = ","
    }
}
