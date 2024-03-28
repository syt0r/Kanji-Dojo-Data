package export.db

import com.google.gson.Gson

class DatabaseExporter(
    private val database: KanjiDojoData
) {

    fun writeStrokes(characterToStrokes: List<DatabaseCharacterStrokeData>) = database.transaction {
        characterToStrokes.forEach { (char, strokes) ->
            strokes.forEachIndexed { index, path ->
                database.databaseQueries.insertCharacterStroke(
                    Character_stroke(
                        character = char,
                        stroke_number = index.toLong(),
                        stroke_path = path
                    )
                )
            }
        }
    }

    fun writeKanjiData(kanjiDataList: List<DatabaseKanjiData>) = database.transaction {
        kanjiDataList.forEach { kanjiData ->
            val kanji = kanjiData.kanji

            database.databaseQueries.insertKanjiData(
                Kanji_data(
                    kanji = kanji,
                    frequency = kanjiData.frequency?.toLong(),
                    variantFamily = kanjiData.variantFamily
                )
            )

            val readings = kanjiData.kunReadings.map { DatabaseKanjiReadingType.KUN to it } +
                    kanjiData.onReadings.map { DatabaseKanjiReadingType.ON to it }

            readings.forEach { (readingTypeEnum, readingStr) ->
                database.databaseQueries.insertKanjiReading(
                    Kanji_reading(
                        kanji = kanji,
                        reading_type = readingTypeEnum.value,
                        reading = readingStr
                    )
                )
            }

            kanjiData.meanings.forEachIndexed { priorityValue, meaningValue ->
                database.databaseQueries.insertKanjiMeaning(
                    Kanji_meaning(
                        kanji = kanji,
                        meaning = meaningValue,
                        priority = priorityValue.toLong()
                    )
                )
            }
        }
    }

    fun writeRadicals(radicals: List<DatabaseRadical>) = database.transaction {
        radicals.forEach {
            database.databaseQueries.insertRadical(
                Radical(radical = it.radical, strokesCount = it.strokes.toLong())
            )
        }
    }


    fun writeKanjiRadicals(data: List<DatabaseKanjiRadical>) = database.transaction {
        data.forEach {
            database.databaseQueries.insertKanjiRadical(
                Kanji_radical(
                    kanji = it.kanji,
                    radical = it.radical,
                    start_stroke = it.startPosition.toLong(),
                    strokes_count = it.strokesCount.toLong()
                )
            )
        }
    }

    fun writeExpressions(expressions: List<DatabaseExpression>) = database.transaction {
        val gson = Gson()
        expressions.forEach { expression ->

            database.databaseQueries.insertExpression(Expression(expression.id))

            expression.readings.forEach {
                database.databaseQueries.insertExpressionReading(
                    Expression_reading(
                        expression_id = expression.id,
                        expression = it.kanjiReading,
                        kana_expression = it.kanaReading,
                        furigana = it.furigana?.let { gson.toJson(it) },
                        rank = it.rank.toLong()
                    )
                )
            }

            expression.meanings.forEachIndexed { index, meaning ->
                database.databaseQueries.insertExpressionMeaning(
                    Expression_meaning(
                        expression_id = expression.id,
                        meaning = meaning,
                        priority = index.toLong()
                    )
                )
            }
        }
    }

}