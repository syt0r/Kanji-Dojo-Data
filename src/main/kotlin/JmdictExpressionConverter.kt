import export.json.ExpressionReading
import export.json.JsonExpressionData
import export.json.LocalizedJsonStrings
import parser.JMDictElementType
import parser.JMdictItem

object JmdictExpressionConverter {

    fun convert(
        jMdictItem: JMdictItem,
        furiganaProvider: FuriganaProvider
    ): JsonExpressionData {

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
            .flatMap { kanjiReading ->
                kanaReadings.map { kanaReading -> kanjiReading.expression to kanaReading.kanaExpression!! }
            }
            .mapNotNull { (kanjiReading, kanaReading) ->
                val furigana = furiganaProvider.getFuriganaForReading(kanjiReading, kanaReading)
                    ?: return@mapNotNull null

                ExpressionReading(
                    kanjiExpression = kanjiReading,
                    kanaExpression = null,
                    furiganaExpression = furigana,
                    rank = null
                )
            }

        val readings = (kanaReadings + kanjiReadings)

        if (readings.isEmpty()) throw IllegalStateException("No readings for ${jMdictItem.entrySequence} expression")

        return JsonExpressionData(
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

}