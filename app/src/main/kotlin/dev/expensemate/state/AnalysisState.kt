package dev.expensemate.state

import dev.expensemate.domain.AnalysisPeriod

class AnalysisState {
    var analysisPeriod = AnalysisPeriod.MONTH
    var analysisTag: String? = null
    var recordSort = 0
}
