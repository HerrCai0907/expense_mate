package com.example.expensemate

data class TagPriority(
    val tag: String,
    val priority: Int
)

class TagPriorityRanker(
    private val weights: List<Int> = DEFAULT_WEIGHTS
) {
    fun rank(tags: Collection<String>, lruOrder: Collection<String>): List<TagPriority> {
        val normalizedTags = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val knownTags = normalizedTags.toSet()
        val orderedByRecentUse = lruOrder.map { it.trim() }
            .filter { it.isNotEmpty() && it in knownTags }
            .distinct()

        val untrackedTags = normalizedTags.filterNot { it in orderedByRecentUse }
        return (orderedByRecentUse + untrackedTags).mapIndexed { index, tag ->
            TagPriority(tag, weights.getOrElse(index) { 0 })
        }
    }

    companion object {
        private val DEFAULT_WEIGHTS = listOf(100, 80, 70, 60, 50, 20, 20, 20, 20, 20)
    }
}
