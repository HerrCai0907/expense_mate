package dev.expensemate.ui.entry

import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import dev.expensemate.data.TagStore
import dev.expensemate.state.EntryState
import dev.expensemate.ui.UiComponents

class TagDropdown(
    private val ui: UiComponents,
    private val state: EntryState,
    private val store: TagStore
) {
    private val context get() = ui.context

    fun render(spinner: Spinner) {
        val tags = store.rankedTags()
        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, tags) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                return tagOptionView(getItem(position).orEmpty())
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                return tagOptionView(getItem(position).orEmpty())
            }
        }
        spinner.adapter = adapter
        if (tags.isEmpty()) {
            spinner.isEnabled = false
            state.selectedTags.clear()
            spinner.onItemSelectedListener = null
            return
        }

        spinner.isEnabled = true
        val selectedTag = state.selectedTags.firstOrNull()?.takeIf { it in tags } ?: tags.first()
        state.selectedTags.clear()
        state.selectedTags += selectedTag
        spinner.setSelection(tags.indexOf(selectedTag), false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                state.selectedTags.clear()
                state.selectedTags += tags[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun tagOptionView(label: String): TextView {
        return TextView(context).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            minHeight = ui.dp(48)
            setPadding(ui.dp(14), 0, ui.dp(14), 0)
            setTextColor(ui.palette.textPrimary)
            setBackgroundColor(ui.palette.inputSurface)
        }
    }
}
