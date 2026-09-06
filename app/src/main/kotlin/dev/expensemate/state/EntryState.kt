package dev.expensemate.state



class EntryState {
    var draftAmount = ""
    var draftDetail = ""
    var draftCustomTag = ""
    val selectedTags = linkedSetOf<String>()
    var selectedExpenseTimeMillis: Long? = null
}
