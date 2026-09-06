package dev.expensemate

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import dev.expensemate.data.ExpenseDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScreenFlowTest {
    private fun descendants(view: View): List<View> = buildList {
        add(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) addAll(descendants(view.getChildAt(index)))
    }

    private fun views(activity: MainActivity) = descendants(activity.window.decorView)
    private fun input(activity: MainActivity, hint: String) = views(activity).filterIsInstance<EditText>()
        .single { it.hint?.toString() == hint }
    private fun click(activity: MainActivity, text: String) {
        views(activity).filterIsInstance<Button>().single { it.text.toString() == text }.performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun navigate(activity: MainActivity, label: String) {
        views(activity).filterIsInstance<ImageButton>().single { it.contentDescription == label }.performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun hasText(activity: MainActivity, text: String) = views(activity).filterIsInstance<TextView>()
        .any { it.text.toString() == text }

    @Test fun draftSurvivesSettingsThemeChangeAndActivityRecreation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        input(activity, "金额").setText("42.50")
        input(activity, "手动输入标签").setText("餐饮")
        click(activity, "添加")
        click(activity, "详细")
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        descendants(dialog.window!!.decorView).filterIsInstance<EditText>().single().setText("午餐备注")
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        navigate(activity, "设置")
        click(activity, "夜间")
        assertTrue(hasText(activity, "时间预设"))
        val saved = Bundle()
        controller.saveInstanceState(saved).pause().stop().destroy()
        val restoredController = Robolectric.buildActivity(MainActivity::class.java)
            .create(saved).start().restoreInstanceState(saved).resume().visible()
        try {
            val restored = restoredController.get()
            assertTrue(hasText(restored, "时间预设"))
            click(restored, "返回录入")
            assertEquals("42.50", input(restored, "金额").text.toString())
            assertTrue(hasText(restored, "详细（已填写）"))
            assertEquals("餐饮", views(restored).filterIsInstance<Spinner>().single().selectedItem)
            assertEquals("NIGHT", restored.getSharedPreferences("expense_mate", 0).getString("appearance_mode", null))
        } finally {
            restoredController.pause().stop().destroy()
        }
    }

    @Test fun saveRecordAnalysisFiltersAndBackNavigationRemainConnected() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        try {
            input(activity, "金额").setText("18.80")
            input(activity, "手动输入标签").setText("餐饮")
            click(activity, "保存账单")
            assertEquals("", input(activity, "金额").text.toString())
            val database = ExpenseDatabase(activity)
            try {
                assertEquals(1880L, database.expensesSince(0).single().amountCents)
            } finally {
                database.close()
            }
            navigate(activity, "分析")
            assertTrue(hasText(activity, "支出分析"))
            val period = views(activity).filterIsInstance<Spinner>().first()
            period.setSelection(4) // ALL
            shadowOf(Looper.getMainLooper()).idle()
            click(activity, "查看全部 1 笔记录 →")
            assertTrue(hasText(activity, "支出记录"))
            val selectors = views(activity).filterIsInstance<Spinner>()
            assertEquals(4, selectors.first().selectedItemPosition)
            selectors.last().setSelection(2)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(2, views(activity).filterIsInstance<Spinner>().last().selectedItemPosition)
            activity.onBackPressed()
            assertTrue(hasText(activity, "支出分析"))
            activity.onBackPressed()
            assertTrue(hasText(activity, "保存账单"))
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
