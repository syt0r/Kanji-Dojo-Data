package task

import ProjectData
import export.json.*
import parser.*
import java.util.concurrent.atomic.AtomicInteger

private const val MinimumCharacterCoverageExpressionsCount = 5

fun main() {

    val jMdictItems = JMdictParser.Instance.parse(ProjectData.jMdictFile).asSequence()

    val readingsMap: Map<FuriganaExpressionReading, FuriganaExpression> = JMdictFuriganaParser
        .parse(ProjectData.furiganaFile)
        .associate {
            val reading = FuriganaExpressionReading(it.kanjiExpression, it.kanaExpression)
            reading to FuriganaExpression(it.furigana)
        }

    val popularExpressions = jMdictItems.takeOnlyWithClassifiedReadings()
        .mapToJson(readingsMap)
        .toList()

    println("Popular expressions found: ${popularExpressions.size}")

    val characterCoverage: Map<Char, AtomicInteger> = ProjectData.exportCharactersDir.listFiles()!!
        .map { it.nameWithoutExtension.first() }
        .associateWith { AtomicInteger(0) }

    fun updateCoverage(characters: List<Char>) {
        characters.forEach { characterCoverage[it]?.incrementAndGet() }
    }

    popularExpressions.forEach { updateCoverage(it.readings.getAllCharacters()) }

    val popularExpressionIds = popularExpressions.map { it.id }.toSet()

    val extraCoverageExpressions = jMdictItems
        .filter { !popularExpressionIds.contains(it.entrySequence) }
        .mapToJson(readingsMap)
        .filter { expressionData ->
            val characters = expressionData.readings.getAllCharacters()
            val coversMore = characters.mapNotNull { characterCoverage[it]?.get() }
                .any { it < MinimumCharacterCoverageExpressionsCount }

            if (coversMore) updateCoverage(characters)

            coversMore
        }
        .toList()

    println("Extra coverage expressions found: ${extraCoverageExpressions.size}")

    val exportExpressions = popularExpressions + extraCoverageExpressions

    println("Total expressions count: ${exportExpressions.size}")

    val charactersWithoutFullCoverage = characterCoverage
        .filter { it.value.get() < MinimumCharacterCoverageExpressionsCount }
        .keys

    println(
        "${charactersWithoutFullCoverage.size} characters without enough coverage: "
                + charactersWithoutFullCoverage.joinToString("")
    )

    JsonExporter.exportExpressions(expressions = exportExpressions)

}

private data class FuriganaExpressionReading(
    val kanji: String,
    val kana: String
)

private data class FuriganaExpression(
    val elements: List<JMDictFuriganaRubyItem>
)

private fun Sequence<JMdictItem>.takeOnlyWithClassifiedReadings(): Sequence<JMdictItem> = filter { jMdictItem ->
    jMdictItem.elements.any { it.priorities.isNotEmpty() }
}

private fun Sequence<JMdictItem>.mapToJson(
    readingsMap: Map<FuriganaExpressionReading, FuriganaExpression>
): Sequence<JsonExpressionData> = map { jMdictItem ->

    val kanaReadings = jMdictItem.elements
        .filter { it.type == JMDictElementType.Reading }
        .map {
            ExpressionReading(
                kanaExpression = it.expression,
                kanjiExpression = null,
                furiganaExpression = null,
                rank = null
            )
        }

    val kanjiReadings = jMdictItem.elements
        .filter { it.type == JMDictElementType.Kanji }
        .map { it.expression }
        .mapNotNull innerMapNotNull@{ kanjiReading ->

            val furigana = kanaReadings.firstNotNullOfOrNull { kanaExpressionReading ->
                val kanaReading = kanaExpressionReading.kanaExpression!!
                readingsMap[FuriganaExpressionReading(kanjiReading, kanaReading)]
            } ?: return@innerMapNotNull null

            ExpressionReading(
                kanjiExpression = kanjiReading,
                kanaExpression = null,
                furiganaExpression = furigana.elements.map { FuriganaElement(it.ruby, it.rt) },
                rank = null
            )
        }

    val readings = (kanaReadings + kanjiReadings)

    if (readings.isEmpty()) throw IllegalStateException("No readings for ${jMdictItem.entrySequence} expression")

    JsonExpressionData(
        id = jMdictItem.entrySequence,
        readings = readings,
        meanings = jMdictItem.glossaryItems
            .groupBy { it.language }
            .map { (language, items) ->
                LocalizedJsonStrings(
                    locale = language,
                    values = items.map { it.text }.distinct()
                )
            }
    )

}

private fun List<ExpressionReading>.getAllCharacters(): List<Char> {
    return this
        .flatMap {
            val kanaChars = it.kanaExpression?.toCharArray()?.toList() ?: emptyList()
            val kanjiChars = it.kanjiExpression?.toCharArray()?.toList() ?: emptyList()
            kanaChars + kanjiChars
        }
        .distinct()
}