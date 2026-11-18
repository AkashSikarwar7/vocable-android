package com.willowtree.vocable.locale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pins Hindi (`values-hi-rIN`) as a complete translation of `values/strings.xml`.
 *
 * Before this test the Hindi file existed but held English copies for 156 of its 165 strings, and
 * nothing noticed: the file opts out of lint with `tools:ignore="MissingTranslation"`, and an English
 * copy is not "missing" anyway. Preset phrases are resolved by resource name at runtime
 * (`Phrase.text()` → `getIdentifier`), so an untranslated `preset_*` string silently ships English to
 * a Hindi user. Same off-disk XML approach as `VoiceGridResourcesTest`, since there's no Robolectric.
 */
class HindiResourcesTest {

    private val resDir: File by lazy {
        val candidates = generateSequence(File("").absoluteFile) { it.parentFile }
            .flatMap { sequenceOf(File(it, "src/main/res"), File(it, "app/src/main/res")) }
        candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/res from ${File("").absolutePath}")
    }

    private data class Entry(val text: String, val translatable: Boolean)

    private fun stringsIn(valuesDir: String): Map<String, Entry> {
        val file = File(resDir, "$valuesDir/strings.xml")
        assertTrue("Missing $valuesDir/strings.xml", file.isFile)
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            .getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val element = nodes.item(index) as Element
            element.getAttribute("name") to Entry(
                text = element.textContent.trim(),
                translatable = element.getAttribute("translatable") != "false"
            )
        }
    }

    private val english by lazy { stringsIn("values").filterValues { it.translatable } }
    private val hindi by lazy { stringsIn("values-hi-rIN") }

    private val devanagari = Regex("[\\u0900-\\u097F]")
    private val latinLetter = Regex("[A-Za-z]")
    private val placeholder = Regex("%(?:\\d+\\$)?\\d*[sd]")

    @Test
    fun `every translatable English string has a Hindi entry`() {
        val missing = english.keys - hindi.keys
        assertTrue("Missing from values-hi-rIN: $missing", missing.isEmpty())
    }

    @Test
    fun `Hindi has no strings that English no longer defines`() {
        val stale = hindi.keys - english.keys
        assertTrue("Stale or untranslatable keys in values-hi-rIN: $stale", stale.isEmpty())
    }

    @Test
    fun `Hindi strings are actually translated, not English copies`() {
        // A string with any Latin letter in English must have Devanagari in Hindi. Pure numerals and
        // symbols ("123") are exempt; brand names may stay Latin alongside Hindi ("Vocable रीसेट करें").
        val untranslated = english.filter { (name, entry) ->
            latinLetter.containsMatchIn(entry.text) &&
                hindi[name]?.let { !devanagari.containsMatchIn(it.text) } == true
        }.keys
        assertTrue("Still English in values-hi-rIN: $untranslated", untranslated.isEmpty())
    }

    @Test
    fun `Hindi keeps the same format placeholders as English`() {
        english.forEach { (name, entry) ->
            val hindiText = hindi[name]?.text ?: return@forEach
            assertEquals(
                "Placeholder mismatch in $name",
                placeholder.findAll(entry.text).map { it.value }.sorted().toList(),
                placeholder.findAll(hindiText).map { it.value }.sorted().toList()
            )
        }
    }
}
