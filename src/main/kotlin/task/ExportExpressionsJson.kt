package task

import ProjectData
import export.json.*
import parser.*

fun main() {

    val jMdictItems = JMdictParser.Instance.parse(ProjectData.jMdictFile)
    val kanjiWithKanaToFuriganaMap = JMdictFuriganaParser.parse(ProjectData.furiganaFile)
        .associate { (it.kanjiExpression to it.kanaExpression) to it.furigana }

    val exportExpressions: List<JsonExpressionData> = extractValidExpressions(
        jMdictItems = jMdictItems,
        kanjiWithKanaToFuriganaMap = kanjiWithKanaToFuriganaMap
    )

    JsonExporter.exportExpressions(
        expressions = exportExpressions,
        mergeExistingData = true
    )

}

private fun extractValidExpressions(
    jMdictItems: List<JMdictItem>,
    kanjiWithKanaToFuriganaMap: Map<Pair<String, String>, List<JMDictFuriganaRubyItem>>
): List<JsonExpressionData> {
    return jMdictItems.mapNotNull { jMdictItem ->

        val kanaReadings = jMdictItem.elements.filter { it.type == JMDictElementType.Reading }
            .map {
                ExpressionReading(
                    kanaExpression = it.expression,
                    ranking = it.priority,
                    kanjiExpression = null,
                    furiganaExpression = null
                )
            }

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
}