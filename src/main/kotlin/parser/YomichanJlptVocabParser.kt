package parser

import export.db.Vocab_imports
import export.db.Vocab_kana_element
import export.db.Vocab_kanji_element
import java.io.File

class YomichanJlptVocabParser {

    fun parse(
        folder: File,
        kanjiElements: List<Vocab_kanji_element>,
        kanaElements: List<Vocab_kana_element>
    ): List<Vocab_imports> {

        val kanjiGroups = kanjiElements.groupBy { it.entry_id }
        val kanaGroups = kanaElements.groupBy { it.entry_id }

        val lineRegex = Regex(
            "^(\\d+),(\"(?:[^\"]|\"\")*\"|),(\"(?:[^\"]|\"\")*\"),(\"(?:[^\"]|\"\")*\"),(\"(?:[^\"]|\"\")*\"),(\"(?:[^\"]|\"\")*\")?$"
        )

        return folder.listFiles()!!
            .filter { it.extension == "csv" }
            .flatMap { file ->
                file.readLines()
                    .drop(1) // header
                    .mapNotNull {
                        runCatching { lineRegex.find(it) }.getOrElse { System.err.println(it.message);null }
                    }
                    .mapNotNull {
                        val origin = it.groupValues[5].removeQuotationMarks()

                        if (origin != "waller") {
                            println("Skip non waller line ${it.groupValues[0]}")
                            return@mapNotNull null
                        }

                        val id = it.groupValues[1].removeQuotationMarks().toLong()
                        val kanji = it.groupValues[2].takeIf { it.isNotEmpty() }?.removeQuotationMarks()
                        val kana = it.groupValues[3].removeQuotationMarks()
                        val definition = it.groupValues[4].removeQuotationMarks()

                        val priority = when {
                            kanji != null -> kanjiGroups[id]?.find { it.reading == kanji }?.priority
                            else -> kanaGroups[id]?.find { it.reading == kana }?.priority
                        }

                        Vocab_imports(
                            jmdict_seq = id,
                            kanji = kanji,
                            kana = kana,
                            definition = definition,
                            priority = priority,
                            class_ = file.nameWithoutExtension
                        )
                    }
            }
    }

    private fun String.removeQuotationMarks(): String = removeSurrounding("\"")

}