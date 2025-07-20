package task

import ProjectData
import export.json.SentenceData
import export.json.SentenceStatsCategory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import parser.IchiranParser
import parser.TextAnalysisNode

fun TextAnalysisNode.words(): List<TextAnalysisNode.Word> = when (this) {
    is TextAnalysisNode.AlternativeGroup -> childNodeList.first().words()
    is TextAnalysisNode.Compound -> childNodeList.flatMap { it.words() }
    is TextAnalysisNode.Word -> listOf(this)
    is TextAnalysisNode.Error,
    is TextAnalysisNode.Text -> listOf()
}

fun TextAnalysisNode.Word.toFuriganaItem(): List<SentenceData.FuriganaItem> {
    return reading.furigana
        ?.let { it.compounds.map { SentenceData.FuriganaItem(it.text, it.annotation) } }
        ?: listOf(SentenceData.FuriganaItem(reading.kanaReading))
}

fun TextAnalysisNode.toFurigana(): List<SentenceData.FuriganaItem> {
    return when (this) {
        is TextAnalysisNode.AlternativeGroup -> childNodeList.first().toFurigana()
        is TextAnalysisNode.Compound -> childNodeList.flatMap { it.toFurigana() }
        is TextAnalysisNode.Word -> toFuriganaItem()
        is TextAnalysisNode.Error -> {
            System.err.println(this.text)
            listOf()
        }

        is TextAnalysisNode.Text -> listOf(SentenceData.FuriganaItem(value))
    }
}

fun main() {
    val jlptLevelToIdSet = getVocabImports()
        .groupBy { it.deck }
        .mapValues { it.value.map { it.jmdict_seq }.toSet() }

    val jlptCategories = SentenceStatsCategory.entries
        .minus(SentenceStatsCategory.other)

    val otherCategory = SentenceStatsCategory.other

    val json = Json {
        prettyPrint = true
        prettyPrintIndent = "\t"
    }

    val sentences = ProjectData.sentencesDir.listFiles()
        .map {
            val data = Json.decodeFromString<SentenceData>(it.readText())
            val nodes = IchiranParser.invoke(data.ichiran)
            val mainWords = nodes
                .flatMap { it.words() }
                .filterNot { it.cards.any { it.partOfSpeech.contains(TextAnalysisNode.PartOfSpeech.Particle) } }

            val stats = mutableMapOf<SentenceStatsCategory, Int>()

            fun incrementCategory(category: SentenceStatsCategory) {
                stats[category] = stats[category]?.plus(1) ?: 1
            }

            mainWords.forEach { word ->
                if (word.sequence == null) return@forEach

                val matches = jlptCategories.filter { jlptLevelToIdSet[it.name]!!.contains(word.sequence) }

                if (matches.isEmpty()) {
                    incrementCategory(otherCategory)
                } else {
                    matches.forEach { incrementCategory(it) }
                }
            }

            val score = stats.asIterable()
                .sumOf { (category, count) -> category.score * count }
                .toFloat()
                .div(stats.asIterable().sumOf { it.value })

            val updatedData = data.copy(
                stats = stats.mapKeys { it.key.name },
                score = score,
                furigana = nodes.flatMap { it.toFurigana() }
            )

            it.writeText(json.encodeToString(updatedData))
            updatedData
        }

    println(sentences.size)

}