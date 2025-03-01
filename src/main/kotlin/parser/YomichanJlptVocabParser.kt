package parser

import org.apache.commons.csv.CSVFormat
import java.io.File

class YomichanJlptVocabParser {

    data class Item(
        val jlpt: String,
        val id: Long,
        val kanji: String?,
        val kana: String,
        val definition: String
    )

    fun parse(folder: File): List<Item> {
        val csvParser = CSVFormat.Builder.create().get()

        return folder.listFiles()!!
            .filter { it.extension == "csv" }
            .flatMap { file ->
                csvParser
                    .parse(file.reader())
                    .toList()
                    .drop(1)
                    .map { file.nameWithoutExtension to it.values() }
            }
            .mapNotNull { (fileName, values) ->

                val id = values[0].toLongOrNull()
                    ?: return@mapNotNull run { System.err.println("No id for ${values.joinToString()}"); null }
                val kana = values[1]
                val kanji = values[2].takeIf { it.isNotEmpty() }
                val definition = values[3]

                Item(
                    jlpt = fileName,
                    id = id,
                    kanji = kanji,
                    kana = kana,
                    definition = definition
                )
            }
    }

}