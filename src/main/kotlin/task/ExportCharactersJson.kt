package task

import AllKanaForExport
import ProjectData
import export.json.JsonCharacterData
import export.json.JsonExporter
import export.json.JsonKanjiRadicalData
import export.json.LocalizedJsonStrings
import parser.CharacterWritingData
import parser.KanjiDicEntry
import parser.KanjiDicParser
import parser.KanjiVgParser

fun main() {
    val writingDataList = KanjiVgParser.Instance.parse(ProjectData.kanjiVGDir)
    val writingDataMap = writingDataList.associateBy { it.character.toString() }
    val kanjiDicEntries = KanjiDicParser.Instance.parse(ProjectData.kanjiDicFile)

    val kanaExportData: List<JsonCharacterData.Kana> = AllKanaForExport.map {
        val kana = it.toString()
        JsonCharacterData.Kana(
            value = kana,
            strokes = writingDataMap.getValue(kana).strokes
        )
    }

    val kanjiExportData: List<JsonCharacterData.Kanji> = extractValidKanjiForExport(
        kanjiDicEntries = kanjiDicEntries,
        writingDataMap = writingDataMap
    )

    JsonExporter.exportCharacters(characters = kanaExportData + kanjiExportData)
}

private fun extractValidKanjiForExport(
    kanjiDicEntries: List<KanjiDicEntry>,
    writingDataMap: Map<String, CharacterWritingData>
): List<JsonCharacterData.Kanji> {
    return kanjiDicEntries.mapNotNull { kanjiDicEntry ->
        val writingData = writingDataMap[kanjiDicEntry.character] ?: return@mapNotNull null
        kanjiDicEntry.run {
            JsonCharacterData.Kanji(
                value = character,
                strokes = writingData.strokes,
                kunReadings = kunReadings,
                onReadings = onReadings,
                frequency = frequency,
                meanings = meanings.groupBy { it.language }
                    .map { (locale, meanings) ->
                        LocalizedJsonStrings(
                            locale = locale ?: "en",
                            values = meanings.map { it.value }
                        )
                    },
                variantsFamily = null, // Using separate task to set
                radicals = writingData.allRadicals
                    .map { characterRadical ->
                        JsonKanjiRadicalData(
                            radical = characterRadical.radical,
                            startStroke = characterRadical.startPosition,
                            strokes = characterRadical.strokesCount,
                            variant = characterRadical.variant.takeIf { it },
                            part = characterRadical.part
                        )
                    }
                    .takeIf { it.isNotEmpty() }
            )
        }
    }
}
