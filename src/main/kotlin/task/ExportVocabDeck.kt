package task

import ProjectData
import export.json.JsonExporter
import export.json.JsonVocabDeckItem
import isKana
import isKanji
import parser.JMdictItem
import parser.JMdictParser
import java.io.File

fun main(args: Array<String>) {
    val vocabDeckFileName = args.firstOrNull() ?: "default.csv"
    val file = File(ProjectData.rawVocabDecksDir, vocabDeckFileName)
    val input = file.readText()

    val lookupVocabItems: List<LookupVocabData> = input.split("\n")
        .map { line ->
            val lineElements = line.split("\\s+|/|,".toRegex())
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val doesContainJapaneseToElementsMap = lineElements.groupBy {
                it.any { it.isKanji() || it.isKana() }
            }
            LookupVocabData(
                readings = doesContainJapaneseToElementsMap.getValue(true),
                meanings = doesContainJapaneseToElementsMap[false] ?: emptyList()
            )
        }

    val jmdictItems = JMdictParser.Instance.parse(ProjectData.jMdictFile)
    val readingToJmdictItems: Map<String, List<LookupJmdictData>> = jmdictItems
        .flatMap { i -> i.elements.map { it.expression to i } }
        .groupBy { (reading, _) -> reading }
        .toList()
        .associate { (reading, groupItems) ->
            reading to groupItems.map { (_, jmdictItem) ->
                LookupJmdictData(
                    readings = jmdictItem.elements.map { it.expression }.toSet(),
                    item = jmdictItem
                )
            }
        }

    val lookupResults = lookupVocabItems.associateWith { lookupVocabData ->
        val lookupReadings = lookupVocabData.readingsSet
        val jmdictLookupItems = lookupReadings.flatMap { readingToJmdictItems[it] ?: emptyList() }
        val fullMatchJmdictData = jmdictLookupItems.firstOrNull { it.readings.containsAll(lookupReadings) }
        when {
            fullMatchJmdictData != null -> {
                LookupResult.FullMatch(lookupReadings, fullMatchJmdictData.item)
            }

            jmdictLookupItems.isEmpty() -> {
                LookupResult.NoMatch(lookupReadings)
            }

            else -> {
                LookupResult.PartialMatches(
                    readings = lookupReadings,
                    items = jmdictLookupItems.map { it.item }
                )
            }
        }
    }

    println("fullMatches[${lookupResults.filterValues { it is LookupResult.FullMatch }.size}]")
    println("partialMatches[${lookupResults.filterValues { it is LookupResult.PartialMatches }.size}]")
    println("noMatches[${lookupResults.filterValues { it is LookupResult.NoMatch }.size}]")

    val exportItems = lookupResults.map { (data, result) ->
        JsonVocabDeckItem(
            readings = data.readings,
            meanings = data.meanings,
            id = when (result) {
                is LookupResult.FullMatch -> listOf(result.item.entrySequence.toLong())
                is LookupResult.PartialMatches -> result.items.map { it.entrySequence.toLong() }
                is LookupResult.NoMatch -> null
            }
        )
    }

    JsonExporter.exportVocabDeck(
        title = file.nameWithoutExtension,
        items = exportItems
    )
}

private data class LookupVocabData(
    val readings: List<String>,
    val meanings: List<String>
) {
    val readingsSet: Set<String> = readings.toSet()
}

private data class LookupJmdictData(
    val readings: Set<String>,
    val item: JMdictItem
)

private sealed interface LookupResult {

    val readings: Set<String>

    data class FullMatch(
        override val readings: Set<String>,
        val item: JMdictItem
    ) : LookupResult

    data class PartialMatches(
        override val readings: Set<String>,
        val items: List<JMdictItem>
    ) : LookupResult

    data class NoMatch(
        override val readings: Set<String>
    ) : LookupResult

}