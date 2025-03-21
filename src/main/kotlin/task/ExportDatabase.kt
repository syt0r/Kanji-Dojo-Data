package task

import ProjectData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import export.db.*
import export.json.JsonCharacterData
import org.apache.commons.csv.CSVFormat
import parser.CompositeJMdictParser
import parser.RadkFileParser
import java.io.File

const val ExportFileNameTemplate = "kanji-dojo-data-base-v%d.sql"
const val ExportDatabaseVersion = 14

private const val MaxExamplesPerLetter = 5

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

    val exportLetterVocabExamples = readExportVocabExamples()

    val supportedVocabIdSet = ProjectData.supportedVocab
        .readLines()
        .map { it.toLong() }
        .toSet()

    val exportVocabData = CompositeJMdictParser.parse(supportedVocabIdSet)
    val exportVocabDeckCards: List<Vocab_deck_card> = getVocabImports()

    assertVocabData(exportVocabData, supportedVocabIdSet)
    assertVocabDeckCards(exportVocabDeckCards, supportedVocabIdSet)

    DatabaseExporter(
        file = File(ExportFileNameTemplate.format(ExportDatabaseVersion)),
        version = ExportDatabaseVersion
    ).apply {
        writeStrokes(exportStrokesData)
        writeKanjiData(exportKanjiData)
        writeKanjiRadicals(exportKanjiRadicals)
        writeRadicals(exportRadicals)
        writeKanjiClassifications(exportKanjiClassifications)
        writeLetterVocabExamples(exportLetterVocabExamples)
        writeVocab(exportVocabData)
        writeVocabDeckCards(exportVocabDeckCards)
    }

}

private fun getVocabImports(): List<Vocab_deck_card> {
    val csvFormat = CSVFormat.Builder.create().get()
    return ProjectData.exportVocabDecksDir.listFiles()!!
        .flatMap { file -> csvFormat.parse(file.reader()).toList().map { file.nameWithoutExtension to it.values() } }
        .map { (fileName, values) ->
            Vocab_deck_card(
                jmdict_seq = values[0].toLong(),
                kanji = values[1].takeIf { it.isNotEmpty() },
                kana = values[2],
                definition = values.getOrNull(3),
                priority = null,
                deck = fileName
            )
        }
}

private fun readExportVocabExamples(): List<Letter_vocab_example> {
    val typeToken = object : TypeToken<List<LetterRepresentationItem>>() {}
    return Gson().fromJson(ProjectData.letterVocabExamples.readText(), typeToken).flatMap {
        it.vocabExamples.take(MaxExamplesPerLetter).map { vocab ->
            Letter_vocab_example(
                letter = it.letter,
                vocab_id = vocab.id,
                kanji = vocab.kanjiReading,
                kana = vocab.kanaReading
            )
        }
    }
}

private fun assertVocabData(exportVocabData: DatabaseVocabData, supportedVocabIdSet: Set<Long>) {
    val actualWords = exportVocabData.entries.map { it.id }.toSet()
    if (actualWords != supportedVocabIdSet) {
        val missing = supportedVocabIdSet.minus(actualWords)
        val extra = actualWords.minus(supportedVocabIdSet)
        error("Missing supported missing words $missing, extra words $extra")
    }
}

private fun assertVocabDeckCards(vocabDeckCards: List<Vocab_deck_card>, supportedVocabIdSet: Set<Long>) {
    val importedVocabIdSet = vocabDeckCards.map { it.jmdict_seq }.toSet()
    if (!supportedVocabIdSet.containsAll(importedVocabIdSet)) {
        val missing = importedVocabIdSet.minus(supportedVocabIdSet)
        error("Missing vocab used in imports $missing")
    }
}
