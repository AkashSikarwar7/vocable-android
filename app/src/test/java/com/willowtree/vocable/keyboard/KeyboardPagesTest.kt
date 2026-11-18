package com.willowtree.vocable.keyboard

import com.willowtree.vocable.ui.keyboard.KeyboardCell
import com.willowtree.vocable.ui.keyboard.KeyboardCell.Character
import com.willowtree.vocable.ui.keyboard.KeyboardCell.Empty
import com.willowtree.vocable.ui.keyboard.KeyboardCell.PageToggle
import com.willowtree.vocable.ui.keyboard.keyboardPageRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class KeyboardPagesTest {

    private fun chars(vararg keys: String) = keys.map { Character(it) }

    @Test
    fun `single page matches the old chunked grid with a padded last row`() {
        val rows = keyboardPageRows(listOf(listOf("A", "B", "C", "D", "E"), emptyList()), pageIndex = 0, columns = 2)

        assertEquals(
            listOf(chars("A", "B"), chars("C", "D"), listOf(Character("E"), Empty)),
            rows
        )
    }

    @Test
    fun `single page never shows a toggle, even if asked for page 1`() {
        val rows = keyboardPageRows(listOf(listOf("A", "B")), pageIndex = 1, columns = 2)

        assertEquals(listOf(chars("A", "B")), rows)
    }

    @Test
    fun `each page ends with a toggle previewing the other page`() {
        val pages = listOf(listOf("क", "ख", "ग"), listOf("अ", "आ"))

        val first = keyboardPageRows(pages, pageIndex = 0, columns = 4).flatten()
        val second = keyboardPageRows(pages, pageIndex = 1, columns = 4).flatten()

        assertEquals(chars("क", "ख", "ग") + PageToggle("अआ"), first)
        assertEquals(chars("अ", "आ") + PageToggle("कख") + Empty, second)
    }

    @Test
    fun `shorter page is padded to the longer page's row count so keys keep their size`() {
        val pages = listOf(List(9) { "c$it" }, List(2) { "v$it" })

        val first = keyboardPageRows(pages, pageIndex = 0, columns = 5)
        val second = keyboardPageRows(pages, pageIndex = 1, columns = 5)

        assertEquals(2, first.size)
        assertEquals(2, second.size)
        assertEquals(listOf(Empty, Empty, Empty, Empty, Empty), second[1])
        assertTrue((first + second).all { it.size == 5 })
    }

    @Test
    fun `out of range page index falls back to a valid page`() {
        val pages = listOf(listOf("a"), listOf("b"))

        assertEquals(
            keyboardPageRows(pages, pageIndex = 1, columns = 2),
            keyboardPageRows(pages, pageIndex = 7, columns = 2)
        )
    }

    // --- Resource checks: the shipped Hindi layout, at every keyboard_columns breakpoint ---

    private val resDir: File by lazy {
        generateSequence(File("").absoluteFile) { it.parentFile }
            .flatMap { sequenceOf(File(it, "src/main/res"), File(it, "app/src/main/res")) }
            .firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/res from ${File("").absolutePath}")
    }

    private fun parse(path: String) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(resDir, path))

    private fun stringArray(valuesDir: String, name: String): List<String> {
        val arrays = parse("$valuesDir/arrays.xml").getElementsByTagName("string-array")
        val array = (0 until arrays.length).map { arrays.item(it) as Element }
            .firstOrNull { it.getAttribute("name") == name }
            ?: error("No string-array $name in $valuesDir/arrays.xml")
        val items = array.getElementsByTagName("item")
        // Strip Android's resource escapes (\? and \') to compare real key values.
        return (0 until items.length).map { items.item(it).textContent.replace("\\", "") }
    }

    /** Every dir that defines keyboard_columns for a non-locale-specific configuration. */
    private val columnsByBreakpoint: Map<String, Int> by lazy {
        resDir.listFiles { file -> file.isDirectory && file.name.matches(Regex("values(-sw\\d+dp)?(-land)?")) }!!
            .filter { File(it, "integers.xml").isFile }
            .mapNotNull { dir ->
                val integers = parse("${dir.name}/integers.xml").getElementsByTagName("integer")
                (0 until integers.length).map { integers.item(it) as Element }
                    .firstOrNull { it.getAttribute("name") == "keyboard_columns" }
                    ?.let { dir.name to it.textContent.trim().toInt() }
            }
            .toMap()
    }

    private val hindiPages by lazy {
        listOf(stringArray("values-hi", "keyboard_keys"), stringArray("values-hi", "keyboard_keys_page2"))
    }

    @Test
    fun `default locale has no second keyboard page`() {
        assertEquals(emptyList<String>(), stringArray("values", "keyboard_keys_page2"))
    }

    @Test
    fun `Hindi pages cover the full consonant and vowel set exactly once`() {
        val consonants = "कखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसह".map { it.toString() }
        val vowels = "अआइईउऊऋएऐओऔ".map { it.toString() }
        val matras = listOf("ा", "ि", "ी", "ु", "ू", "ृ", "े", "ै", "ो", "ौ")
        val allKeys = hindiPages.flatten()

        assertEquals("Duplicate keys: ${allKeys.groupBy { it }.filter { it.value.size > 1 }.keys}", allKeys.size, allKeys.toSet().size)
        assertTrue("Page 1 must hold every consonant", hindiPages[0].containsAll(consonants))
        assertTrue("Page 2 must hold every vowel and matra", hindiPages[1].containsAll(vowels + matras))
        assertTrue("Halant is needed for conjuncts", "्" in allKeys)
    }

    @Test
    fun `Hindi pages have the same shape at every breakpoint and fit the German row budget`() {
        assertTrue("Found no keyboard_columns breakpoints", columnsByBreakpoint.isNotEmpty())
        columnsByBreakpoint.forEach { (dir, columns) ->
            val first = keyboardPageRows(hindiPages, pageIndex = 0, columns = columns)
            val second = keyboardPageRows(hindiPages, pageIndex = 1, columns = columns)

            assertEquals("Row count differs between pages in $dir", first.size, second.size)
            // German, the largest existing keyboard, is 35 keys = 7 rows on a 5-column phone grid.
            assertTrue("$dir: ${first.size} rows is more than 7", first.size <= 7)
            listOf(first, second).forEach { page ->
                assertEquals("$dir must show exactly one toggle", 1, page.flatten().count { it is PageToggle })
            }
        }
    }

    @Test
    fun `Hindi toggle is the last key on each page`() {
        listOf(0, 1).forEach { pageIndex ->
            val cells: List<KeyboardCell> = keyboardPageRows(hindiPages, pageIndex, columns = 5).flatten()
            assertTrue(cells.last { it != Empty } is PageToggle)
        }
    }
}
