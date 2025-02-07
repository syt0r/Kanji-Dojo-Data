package task

import ProjectData
import com.google.gson.Gson
import export.db.*
import export.json.JsonCharacterData
import parser.CompositeJMdictParser
import parser.RadkFileParser
import parser.YomichanJlptVocabParser
import java.io.File

const val ExportFileNameTemplate = "kanji-dojo-data-base-v%d.sql"
const val ExportDatabaseVersion = 13

fun main() {

    val charactersDir = ProjectData.exportCharactersDir
    val characterFiles = charactersDir.listFiles()
    if (characterFiles.isNullOrEmpty())
        throw IllegalStateException("No characters data found")

    val radicals = RadkFileParser().parse(ProjectData.radkFile)

    val gson = Gson()
    val characters = characterFiles.map { JsonCharacterData.readFromFile(it, gson) }

    val kanjiCharacters = characters.filterIsInstance<JsonCharacterData.Kanji>()

    val exportStrokesData = characters.map { DatabaseCharacterStrokeData(it.value, it.strokes) }

    val exportKanjiData = kanjiCharacters.map {
        val meanings = it.meanings?.firstOrNull { it.locale == "en" }?.values
        if (meanings == null) println("Warning! ${it.value} has no meanings")
        DatabaseKanjiData(
            kanji = it.value,
            meanings = meanings ?: emptyList(),
            onReadings = it.onReadings ?: emptyList(),
            kunReadings = it.kunReadings ?: emptyList(),
            frequency = it.frequency,
            variantFamily = it.variantsFamily
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

    val radicalsInUse = exportKanjiRadicals.map { it.radical }.toSet()
    val filteredRadicals = radicals.filter { it.extraData == null && radicalsInUse.contains(it.radical) }
        .map { it.radical }
        .toSet()

    val filteredOutRadicals = radicals.map { it.radical }.toSet().minus(filteredRadicals)

    println("Filtering out weird and unused radicals [${filteredOutRadicals.size}]: $filteredOutRadicals")

    val exportRadicals: List<DatabaseRadical> = radicals.filter { filteredRadicals.contains(it.radical) }
        .map { DatabaseRadical(it.radical, it.strokes) }

    val exportKanjiClassifications = ProjectData.exportLetterDecksDir.listFiles()!!
        .flatMap { file ->
            file.readText().split("\n").map {
                DatabaseKanjiClassification(
                    kanji = it,
                    classification = file.nameWithoutExtension
                )
            }
        }

    val expressionsDir = ProjectData.exportExpressionsDir
    val expressionFiles = expressionsDir.listFiles()
    if (expressionFiles.isNullOrEmpty())
        throw IllegalStateException("No expressions data found")

    val filteredVocabIdSet = expressionFiles.map { it.nameWithoutExtension.toLong() }.toSet()

    val vocabData = CompositeJMdictParser.parse(filteredVocabIdSet)

    val vocabImports = YomichanJlptVocabParser().parse(ProjectData.yomichanJlptVocabDir)
        .map {
            if (!filteredVocabIdSet.contains(it.id)) error("Expression ${it.id} not exported")
            it.run { DatabaseVocabImport(id, kanji, kana, definition, classification) }
        }

    DatabaseExporter(
        file = File(ExportFileNameTemplate.format(ExportDatabaseVersion)),
        version = ExportDatabaseVersion
    ).apply {
        writeStrokes(exportStrokesData)
        writeKanjiData(exportKanjiData)
        writeKanjiRadicals(exportKanjiRadicals)
        writeRadicals(exportRadicals)
        writeKanjiClassifications(exportKanjiClassifications)
        writeVocab(vocabData)
        writeVocabImports(vocabImports)
    }

}
