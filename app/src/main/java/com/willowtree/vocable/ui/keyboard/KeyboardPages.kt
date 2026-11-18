package com.willowtree.vocable.ui.keyboard

/** One slot in the head-tracking keyboard grid. */
sealed interface KeyboardCell {
    /** Types [value]. */
    data class Character(val value: String) : KeyboardCell

    /** Switches to the next key page; [label] is a preview of the keys on that page. */
    data class PageToggle(val label: String) : KeyboardCell

    /** An empty slot that keeps the remaining keys at their fixed size and position. */
    data object Empty : KeyboardCell
}

/**
 * Lays out the keys of [pageIndex] as fixed-width rows of [columns] cells.
 *
 * A locale whose alphabet doesn't fit one dwell-friendly grid (Hindi: ~60 Devanagari keys) supplies
 * more than one page through `keyboard_keys` / `keyboard_keys_page2`. Every page then ends with a
 * [KeyboardCell.PageToggle], and all pages are padded with [KeyboardCell.Empty] to the same row
 * count. Key size and position therefore don't change when the user flips pages, which matters for
 * gaze users who aim by position (the same fixed-tile contract the phrase grids follow).
 *
 * With a single page the output is identical to the previous `keys.chunked(columns)` grid, with the
 * short last row padded by [KeyboardCell.Empty], so non-paged locales render exactly as before.
 */
fun keyboardPageRows(pages: List<List<String>>, pageIndex: Int, columns: Int): List<List<KeyboardCell>> {
    require(columns > 0) { "columns must be positive, was $columns" }
    val nonEmptyPages = pages.filter { it.isNotEmpty() }
    if (nonEmptyPages.isEmpty()) return emptyList()

    val paged = nonEmptyPages.size > 1
    val cellsPerPage = nonEmptyPages.mapIndexed { index, keys ->
        val cells: List<KeyboardCell> = keys.map { KeyboardCell.Character(it) }
        if (paged) {
            val next = nonEmptyPages[(index + 1) % nonEmptyPages.size]
            cells + KeyboardCell.PageToggle(label = next.take(TOGGLE_PREVIEW_KEYS).joinToString(""))
        } else {
            cells
        }
    }

    val rowCount = cellsPerPage.maxOf { (it.size + columns - 1) / columns }
    val current = cellsPerPage[pageIndex.coerceIn(0, cellsPerPage.lastIndex)]
    val padded = current + List(rowCount * columns - current.size) { KeyboardCell.Empty }
    return padded.chunked(columns)
}

/** Number of keys of the target page shown on the toggle key, e.g. "कख" or "अआ". */
private const val TOGGLE_PREVIEW_KEYS = 2
