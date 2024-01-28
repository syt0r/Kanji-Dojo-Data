package task

import AllKanaForExport
import export.json.JsonCharacterData
import export.json.JsonExporter
import export.json.JsonKanjiRadicalData
import export.json.LocalizedJsonStrings
import parser.KanjiDicParser
import parser.KanjiVgParser
import java.io.File

private val parserDataDir = File("parser_data/")
private val kanjiVGDir = File(parserDataDir, "kanjivg/kanji/")
private val kanjiDicFile = File(parserDataDir, "kanjidic2.xml")

fun main() {
    val writingDataList = KanjiVgParser.Instance.parse(kanjiVGDir)
    val characterToStrokesMap = writingDataList.associateBy { it.character }

    val kanaExportData = AllKanaForExport.map {
        JsonCharacterData.Kana(
            value = it.toString(),
            strokes = characterToStrokesMap.getValue(it).strokes
        )
    }

    val kanjiData = KanjiDicParser.Instance.parse(kanjiDicFile)

    val kanjiExportData = kanjiData.mapNotNull { kanjiDicEntry ->
        val writingData = characterToStrokesMap[kanjiDicEntry.character] ?: return@mapNotNull null
        kanjiDicEntry.run {
            JsonCharacterData.Kanji(
                value = character.toString(),
                kunReadings = kunReadings,
                onReadings = onReadings,
                meanings = meanings.groupBy { it.language }
                    .map { (locale, meanings) ->
                        LocalizedJsonStrings(
                            locale = locale ?: "en",
                            values = meanings.map { it.value }
                        )
                    },
                strokes = writingData.strokes,
                radicals = writingData.allRadicals.map { characterRadical ->
                    JsonKanjiRadicalData(
                        radical = characterRadical.radical,
                        startStroke = characterRadical.startPosition,
                        strokes = characterRadical.strokesCount,
                        variant = characterRadical.variant.takeIf { it },
                        part = characterRadical.part
                    )
                },
                frequency = frequency
            )
        }
    }

    JsonExporter.exportCharacters(
        characters = kanaExportData + kanjiExportData,
        mergeExistingData = true
    )
}
