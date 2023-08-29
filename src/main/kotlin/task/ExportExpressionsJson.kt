package task

import export.json.*
import parser.JMDictElementType
import parser.JMdictFuriganaParser
import parser.JMdictParser
import java.io.File

private val parserDataDir = File("parser_data/")
private val jMdictFile = File(parserDataDir, "JMdict")
private val furiganaFile = File(parserDataDir, "JmdictFurigana.json")

fun main() {
    val jMdictItems = JMdictParser.Instance.parse(jMdictFile)
    val kanjiWithKanaToFuriganaMap = JMdictFuriganaParser.parse(furiganaFile)
        .associate { (it.kanjiExpression to it.kanaExpression) to it.furigana }

    val exportExpressions: List<JsonExpressionData> = jMdictItems.mapNotNull { jMdictItem ->

        val kanaReadings = jMdictItem.elements.filter { it.type == JMDictElementType.Reading }
            .map { ExpressionReading(kanaExpression = it.expression, ranking = it.priority) }

        val kanjiReadings = jMdictItem.elements.filter { it.type == JMDictElementType.Kanji }
            .mapNotNull innerMapNotNull@{ jmDictKanjiElement ->

                val (furigana, kana) = kanaReadings.firstNotNullOfOrNull { kanaReading ->
                    kanjiWithKanaToFuriganaMap[jmDictKanjiElement.expression to kanaReading.kanaExpression]
                        ?.let { it to kanaReading.kanaExpression }
                } ?: return@innerMapNotNull null

                ExpressionReading(
                    kanjiExpression = jmDictKanjiElement.expression,
                    kanaExpression = kana,
                    furiganaExpression = furigana.map { FuriganaElement(it.ruby, it.rt) },
                    ranking = jmDictKanjiElement.priority
                )
            }

        val readings = (kanaReadings + kanjiReadings)
            .filter { it.ranking != null }
            .takeIf { it.isNotEmpty() }
            ?: return@mapNotNull null

        JsonExpressionData(
            id = jMdictItem.entrySequence,
            readings = readings,
            meanings = jMdictItem.glossaryItems.groupBy { it.language }
                .map { (language, items) ->
                    LocalizedJsonStrings(
                        locale = language,
                        values = items.map { it.text }.distinct()
                    )
                }
        )
    }

    JsonExporter.exportExpressions(
        expressions = exportExpressions,
        mergeExistingData = true
    )
}