package task

import ProjectData
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.gson.Gson
import export.db.*
import export.json.JsonCharacterData
import export.json.JsonExpressionData
import parser.RadkFileParser
import java.io.File

const val ExportFileNameTemplate = "kanji-dojo-data-base-v%d.sql"
const val ExportDatabaseVersion = 6

fun main() {

    val charactersDir = ProjectData.exportCharactersDir
    val characterFiles = charactersDir.listFiles()
    if (characterFiles.isNullOrEmpty())
        throw IllegalStateException("No characters data found")

    val expressionsDir = ProjectData.exportExpressionsDir
    val expressionFiles = expressionsDir.listFiles()
    if (expressionFiles.isNullOrEmpty())
        throw IllegalStateException("No expressions data found")

    val radicals = RadkFileParser().parse(ProjectData.radkFile)

    val gson = Gson()
    val characters = characterFiles.map { JsonCharacterData.readFromFile(it, gson) }
    val expressions = expressionFiles.map { gson.fromJson(it.readText(), JsonExpressionData::class.java) }

    val kanjiCharacters = characters.filterIsInstance<JsonCharacterData.Kanji>()

    val exportStrokesData = characters.associate { it.value to it.strokes }

    val exportKanjiData = kanjiCharacters.map {
        val meanings = it.meanings?.firstOrNull { it.locale == "en" }?.values
        if (meanings == null) println("Warning! ${it.value} has no meanings")
        DatabaseKanjiData(
            kanji = it.value,
            meanings = meanings ?: emptyList(),
            onReadings = it.onReadings ?: emptyList(),
            kunReadings = it.kunReadings ?: emptyList(),
            frequency = it.frequency
        )
    }

    val exportKanjiRadicals = kanjiCharacters.flatMap { kanjiData ->
        kanjiData.radicals?.map {
            DatabaseKanjiRadical(
                kanji = kanjiData.value,
                radical = it.radical,
                startPosition = it.startStroke,
                strokesCount = it.strokes
            )
        } ?: emptyList()
    }

    val exportRadicals: List<DatabaseRadical> = radicals.map { DatabaseRadical(it.radical, it.strokes) }

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

private fun JsonExpressionData.toDatabaseExpressionEntity(): DatabaseExpression = DatabaseExpression(
    id = id.toLong(),
    readings = readings.map { reading ->
        DatabaseExpressionReading(
            kanjiReading = reading.kanjiExpression,
            kanaReading = reading.kanaExpression,
            furigana = reading.furiganaExpression?.map { DatabaseFuriganaItem(it.text, it.annotation) },
            rank = reading.ranking ?: Int.MAX_VALUE
        )
    },
    meanings = meanings.first { it.locale == "en" }.values
)
