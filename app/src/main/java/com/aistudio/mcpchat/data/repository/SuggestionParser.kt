package com.aistudio.mcpchat.data.repository

object SuggestionParser {

    const val MARKER = "---SUGGESTIONS---"
    const val MAX_SUGGESTIONS = 3
    const val MAX_SUGGESTION_LENGTH = 80

    private val leadingMarker = Regex("^[\\d]+[.)\\-:\\s]*|^[\\-*\u2022]\\s*")

    fun splitResponse(raw: String): Pair<String, List<String>> {
        val markerIndex = raw.lastIndexOf(MARKER)
        if (markerIndex < 0) return raw to emptyList()
        val clean = raw.substring(0, markerIndex).trimEnd()
        val suggestions = raw.substring(markerIndex + MARKER.length)
            .lines()
            .map { it.trim().replace(leadingMarker, "").trim() }
            .filter { it.isNotBlank() }
            .take(MAX_SUGGESTIONS)
            .map { it.take(MAX_SUGGESTION_LENGTH).trimEnd() }
            .filter { it.isNotBlank() }
        return clean to suggestions
    }
}
