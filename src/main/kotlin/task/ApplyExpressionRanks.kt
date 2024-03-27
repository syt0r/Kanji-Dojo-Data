package task

import ProjectData
import export.json.JsonExporter
import parser.CorpusLeedsParser
import parser.JMdictParser

private const val LeedsMaxRank = 15_000

fun main() {
    val expressionToRank = CorpusLeedsParser.parse(ProjectData.leedsFrequencyFile)
        .associate { it.word to it.wordRank }

    val legacyExpressionToRank = JMdictParser.Instance.parse(ProjectData.jMdictFile)
        .flatMap { it.elements }
        .mapNotNull { it.expression to (LeedsMaxRank + (it.priority ?: return@mapNotNull null)) }
        .toMap()

    JsonExporter.updateExpressions {
        copy(
            readings = readings.map {
                val key = it.kanjiExpression ?: it.kanaExpression
                it.copy(
                    rank = expressionToRank[key] ?: legacyExpressionToRank[key]
                )
            }
        )
    }
}