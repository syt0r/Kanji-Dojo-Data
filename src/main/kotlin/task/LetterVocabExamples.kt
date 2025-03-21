package task

import ProjectData
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

    val moshi = Moshi.Builder()
        .add(CompactVocabExampleAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    val adapter = moshi.adapter<List<LetterRepresentationItem>>(
        Types.newParameterizedType(List::class.java, LetterRepresentationItem::class.java)
    ).indent("\t")

    val extraData = File(ProjectData.exportDir, "output.json")
        .readText()
        .let { adapter.fromJson(it) }!!
        .associateBy { it.letter }

    val currentData = adapter.fromJson(ProjectData.letterVocabExamples.readText())!!

    assert(extraData.keys.toSet() == currentData.map { it.letter }.toSet())

    val updatedData = currentData.map {
        if (it.vocabExamples.isEmpty()) {
            it.copy(vocabExamples = extraData[it.letter]!!.vocabExamples)
        } else it
    }

    val outputJson = adapter.toJson(updatedData)
    ProjectData.letterVocabExamples.writeText(outputJson)

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