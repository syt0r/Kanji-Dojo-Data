package task

import FuriganaProvider
import JmdictExpressionConverter
import ProjectData
import export.json.ExpressionReading
import export.json.JsonExporter
import parser.JMdictItem
import parser.JMdictParser
import java.util.concurrent.atomic.AtomicInteger

private const val MinimumCharacterCoverageExpressionsCount = 5

fun main() {

    val jMdictItems = JMdictParser.Instance.parse(ProjectData.jMdictFile).asSequence()
    val furiganaProvider = FuriganaProvider()

    val popularExpressions = jMdictItems.filterOnlyWithClassifiedReadingPriorities()
        .map { JmdictExpressionConverter.convert(it, furiganaProvider) }
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
        .map { JmdictExpressionConverter.convert(it, furiganaProvider) }
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

private fun Sequence<JMdictItem>.filterOnlyWithClassifiedReadingPriorities(): Sequence<JMdictItem> {
    return filter { jMdictItem -> jMdictItem.elements.any { it.priorities.isNotEmpty() } }
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