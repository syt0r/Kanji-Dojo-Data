package task

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.gson.Gson
import export.db.*
import export.json.JsonCharacterData
import export.json.JsonExporter
import export.json.JsonExpressionData
import java.io.File

const val ExportFileNameTemplate = "kanji-dojo-data-base-v%d.sql"
const val ExportDatabaseVersion = 6

fun main() {

    val charactersDir = JsonExporter.charactersDir
    val characterFiles = charactersDir.listFiles()
    if (characterFiles.isNullOrEmpty())
        throw IllegalStateException("No characters data found")

    val expressionsDir = JsonExporter.expressionsDir
    val expressionFiles = expressionsDir.listFiles()
    if (expressionFiles.isNullOrEmpty())
        throw IllegalStateException("No expressions data found")

    val gson = Gson()
    val characters = characterFiles.map { JsonCharacterData.readFromFile(it, gson) }
    val expressions = expressionFiles.map { gson.fromJson(it.readText(), JsonExpressionData::class.java) }

    val kanjiCharacters = characters.filterIsInstance<JsonCharacterData.Kanji>()

    val exportStrokesData = characters.associate { it.value to it.strokes }
    val exportKanjiData = kanjiCharacters.map {
        val meanings = it.meanings.firstOrNull { it.locale == "en" }?.values
        if (meanings == null) println("Warning! ${it.value} has no meanings")
        DatabaseKanjiData(
            kanji = it.value,
            meanings = meanings ?: emptyList(),
            onReadings = it.onReadings,
            kunReadings = it.kunReadings,
            frequency = it.frequency
        )
    }
    val exportKanjiRadicals = kanjiCharacters.flatMap { kanjiData ->
        kanjiData.radicals.map {
            DatabaseKanjiRadical(
                kanji = kanjiData.value,
                radical = it.radical,
                startPosition = it.startStroke,
                strokesCount = it.strokes
            )
        }
    }
    val exportRadicals: List<DatabaseRadical> = kanjiCharacters.getRadicals()
    val exportExpressions = expressions.map { it.toDatabaseExpressionEntity() }

    val outputDatabaseFile = File(ExportFileNameTemplate.format(ExportDatabaseVersion))
    if (outputDatabaseFile.exists())
        outputDatabaseFile.delete()

    val driver = JdbcSqliteDriver("jdbc:sqlite:${outputDatabaseFile.absolutePath}")

    KanjiDojoData.Schema.create(driver)
    val database = KanjiDojoData(driver)
    driver.execute(
        identifier = null,
        sql = "PRAGMA user_version = $ExportDatabaseVersion;",
        parameters = 0
    )

    DatabaseExporter(database).apply {
        writeStrokes(exportStrokesData)
        writeKanjiData(exportKanjiData)
        writeKanjiRadicals(exportKanjiRadicals)
        writeRadicals(exportRadicals)
        writeExpressions(exportExpressions)
    }

}

private fun List<JsonCharacterData.Kanji>.getRadicals(): List<DatabaseRadical> {
    return flatMap { kanjiData -> kanjiData.radicals.map { it to kanjiData.value } }
        .groupBy { (radicalData, kanji) -> radicalData.radical }
        .flatMap { (radical, radicalVariants) ->

            val radicalVariantStrokesCountToListOjKanji = radicalVariants
                .groupBy { (radicalItem, kanji) -> radicalItem.strokes }

            if (radicalVariantStrokesCountToListOjKanji.size > 1) {
                val message = radicalVariantStrokesCountToListOjKanji
                    .map { (strokesCount, radicalItemToKanji) ->
                        "$strokesCount strokes in characters[${radicalItemToKanji.joinToString("") { it.second }}]"
                    }
                    .joinToString()
                println("Attention! Radical $radical has multiple variants: $message")
            }

            radicalVariants.map { it.first }.distinct()
        }
        .map { DatabaseRadical(radical = it.radical, strokes = it.strokes) }
        .distinct()
}

private fun JsonExpressionData.toDatabaseExpressionEntity(): DatabaseExpression = DatabaseExpression(
    id = id.toLong(),
    readings = readings.map { reading ->
        DatabaseExpressionReading(
            reading.kanjiExpression,
            reading.kanaExpression,
            reading.furiganaExpression.map { DatabaseFuriganaItem(it.text, it.annotation) },
            reading.ranking ?: Int.MAX_VALUE
        )
    },
    meanings = meanings.first { it.locale == "en" }.values
)
