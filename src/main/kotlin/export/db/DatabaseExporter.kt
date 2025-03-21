package export.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

class DatabaseExporter(
    file: File,
    version: Int,
) {

    private val database: KanjiDojoData

    init {
        if (file.exists()) file.delete()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        KanjiDojoData.Schema.create(driver)
        database = KanjiDojoData(driver)
        driver.execute(
            identifier = null,
            sql = "PRAGMA user_version = $version;",
            parameters = 0
        )
    }

    fun writeStrokes(characterToStrokes: List<DatabaseCharacterStrokeData>) = database.transaction {
        characterToStrokes.forEach { (char, strokes) ->
            strokes.forEachIndexed { index, path ->
                database.lettersQueries.insertCharacterStroke(
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

            database.lettersQueries.insertKanjiData(
                Kanji_data(
                    kanji = kanji,
                    frequency = kanjiData.frequency?.toLong(),
                    variantFamily = kanjiData.variantFamily
                )
            )

            val readings = kanjiData.kunReadings.map { DatabaseKanjiReadingType.KUN to it } +
                    kanjiData.onReadings.map { DatabaseKanjiReadingType.ON to it }

            readings.forEach { (readingTypeEnum, readingStr) ->
                database.lettersQueries.insertKanjiReading(
                    Kanji_reading(
                        kanji = kanji,
                        reading_type = readingTypeEnum.value,
                        reading = readingStr
                    )
                )
            }

            kanjiData.meanings.forEachIndexed { priorityValue, meaningValue ->
                database.lettersQueries.insertKanjiMeaning(
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
            database.lettersQueries.insertRadical(
                Radical(radical = it.radical, strokesCount = it.strokes.toLong())
            )
        }
    }


    fun writeKanjiRadicals(data: List<DatabaseKanjiRadical>) = database.transaction {
        data.forEach {
            database.lettersQueries.insertKanjiRadical(
                Kanji_radical(
                    kanji = it.kanji,
                    radical = it.radical,
                    start_stroke = it.startPosition.toLong(),
                    strokes_count = it.strokesCount.toLong()
                )
            )
        }
    }


    fun writeKanjiClassifications(items: List<DatabaseKanjiClassification>) = database.transaction {
        items.forEach {
            database.lettersQueries.insertKanjiClassification(
                Kanji_classification(it.kanji, it.classification)
            )
        }
    }

    fun writeLetterVocabExamples(items: List<Letter_vocab_example>) {
        items.forEach { database.lettersQueries.insertLetterVocabExample(it) }
    }

    fun writeVocab(databaseVocabData: DatabaseVocabData) = database.transaction {
        databaseVocabData.apply {
            entries.forEach { database.vocabQueries.insert_vocab_entry(it) }
            kanjiElements.forEach { database.vocabQueries.insert_vocab_kanji_element(it) }
            kanjiInformation.forEach { database.vocabQueries.insert_vocab_kanji_information(it) }
            kanjiPriorities.forEach { database.vocabQueries.insert_vocab_kanji_priority(it) }
            kanaElements.forEach { database.vocabQueries.insert_vocab_kana_element(it) }
            kanaRestrictions.forEach { database.vocabQueries.insert_vocab_kana_restriction(it) }
            kanaInformation.forEach { database.vocabQueries.insert_vocab_kana_information(it) }
            kanaPriorities.forEach { database.vocabQueries.insert_vocab_kana_priority(it) }
            senses.forEach { database.vocabQueries.insert_vocab_sense(it) }
            senseKanjiRestrictions.forEach { database.vocabQueries.insert_vocab_sense_kanji_restriction(it) }
            senseReadingRestrictions.forEach { database.vocabQueries.insert_vocab_sense_kana_restriction(it) }
            sensePartsOfSpeech.forEach { database.vocabQueries.insert_vocab_sense_part_of_speech(it) }
            senseCrossReferences.forEach { database.vocabQueries.insert_vocab_sense_cross_reference(it) }
            senseAntonyms.forEach { database.vocabQueries.insert_vocab_sense_antonym(it) }
            senseFields.forEach { database.vocabQueries.insert_vocab_sense_field(it) }
            senseMiscellaneous.forEach { database.vocabQueries.insert_vocab_sense_miscellaneous(it) }
            senseDialects.forEach { database.vocabQueries.insert_vocab_sense_dialect(it) }
            senseGlosses.forEach { database.vocabQueries.insert_vocab_sense_gloss(it) }
            senseInformation.forEach { database.vocabQueries.insert_vocab_sense_information(it) }
            senseExample.forEach { database.vocabQueries.insert_vocab_sense_example(it) }
            entities.forEach { database.vocabQueries.insert_vocab_entity(it) }
            furigana.forEach { database.vocabQueries.insert_vocab_furigana(it) }
        }
    }

    fun writeVocabDeckCards(items: List<Vocab_deck_card>) = database.transaction {
        items.forEach { database.vocabQueries.insert_vocab_deck_card(it) }
    }

}