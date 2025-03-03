package task

import NormalHiraganaReadings
import ProjectData
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import hiraganaToKatakana
import isKana
import parser.YomichanJlptVocabParser
import java.io.File

data class LetterRepresentationItem(
    val letter: String,
    val vocabExamples: List<VocabExample>
) {
    data class VocabExample(
        val id: Long,
        val kanjiReading: String?,
        val kanaReading: String
    )
}

private const val ExampleLimit = 15

fun main() {

    val jlptDecksItems = YomichanJlptVocabParser().parse(ProjectData.yomichanJlptVocabDir)
        .groupBy { it.jlpt }
        .toList()
        .sortedByDescending { (jlpt, items) -> jlpt }
        .flatMap { it.second }

    val hiragana = NormalHiraganaReadings.keys
    val katakana = NormalHiraganaReadings.keys.map { hiraganaToKatakana(it) }
    val kana = hiragana.plus(katakana).map { it.toString() }.toSet()

    val jlptOrderedLetters = (5 downTo 1)
        .flatMap { File(ProjectData.exportLetterDecksDir, "n$it.csv").readLines() }
        .toSet()

    val allLetters = ProjectData.exportCharactersDir.listFiles()!!
        .map { it.nameWithoutExtension }
        .sorted()
        .toSet()

    val letters = kana
        .plus(jlptOrderedLetters)
        .plus(allLetters)

    assert(allLetters.size == letters.size)

    val dataList = letters.map { letter ->
        LetterRepresentationItem(
            letter = letter,
            vocabExamples = jlptDecksItems
                .asSequence()
                .filter {
                    if (letter.first().isKana()) it.kana.contains(letter)
                    else it.kanji?.contains(letter) == true
                }
                .take(ExampleLimit)
                .map {
                    LetterRepresentationItem.VocabExample(
                        id = it.id,
                        kanjiReading = it.kanji,
                        kanaReading = it.kana
                    )
                }
                .toList()
        )
    }

    val output = formattedPriorityListJson(dataList)
    File(ProjectData.exportDir, "letter_vocab_priorities.json").writeText(output)

}


private class CompactVocabExampleAdapter {

    @ToJson
    fun toJson(writer: JsonWriter, item: LetterRepresentationItem.VocabExample) {
        val indent = writer.indent
        writer.beginObject()
        writer.indent = ""
        writer.name("id").value(item.id)
        writer.name("kanjiReading").value(item.kanjiReading)
        writer.name("kanaReading").value(item.kanaReading)
        writer.endObject()
        writer.indent = indent
    }

}

private fun formattedPriorityListJson(list: List<LetterRepresentationItem>): String {
    val moshi = Moshi.Builder()
        .add(CompactVocabExampleAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    val adapter = moshi.adapter<List<LetterRepresentationItem>>(
        Types.newParameterizedType(List::class.java, LetterRepresentationItem::class.java)
    ).indent("\t")

    return adapter.toJson(list)
}