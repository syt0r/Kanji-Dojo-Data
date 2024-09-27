package task

import ProjectData
import parser.KanjiDicParser
import parser.KanjiVgParser
import java.io.File

fun main() {
    val items = KanjiDicParser.Instance.parse(ProjectData.kanjiDicFile)
    val supportedLetters = KanjiVgParser.Instance.parse(ProjectData.kanjiVGDir)
        .map { it.character.toString() }
        .toSet()

    val filteredItems = items.filter { supportedLetters.contains(it.character) }

    println("items[${items.size}], supported[${supportedLetters.size}], filtered[${filteredItems.size}]")

    filteredItems.groupBy { it.grade }
        .forEach { (grade, items) ->
            if (grade == null) return@forEach
            writeLetterDeck(
                name = "g$grade",
                letters = items.map { it.character }
            )
        }

}

private fun writeLetterDeck(name: String, letters: List<String>) {
    File(ProjectData.exportLetterDecksDir, "$name.csv").writeText(
        letters.joinToString("\n")
    )
}