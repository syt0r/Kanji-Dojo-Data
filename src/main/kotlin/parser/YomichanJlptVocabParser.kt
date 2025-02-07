package parser

import java.io.File

class YomichanJlptVocabParser {

    data class Item(
        val id: Long,
        val kanji: String?,
        val kana: String,
        val definition: String,
        val classification: String
    )

    fun parse(folder: File): List<Item> {
        return folder.listFiles()!!
            .filter { it.extension == "csv" }
            .flatMap { file ->
                file.readLines()
                    .drop(1) // header
                    .map { it.split(",") }
                    .mapNotNull {
                        runCatching {
                            Item(
                                id = it[0].toLong(),
                                kanji = it[1].takeIf { it.isNotEmpty() }?.removeQuotationMarks(),
                                kana = it[2].removeQuotationMarks(),
                                definition = it[3].removeQuotationMarks(),
                                classification = file.nameWithoutExtension
                            )
                        }.getOrElse {
                            System.err.println(it.message)
                            null
                        }

                    }
            }
    }

    private fun String.removeQuotationMarks(): String = removeSurrounding("\"")

}