package export.db

import com.google.gson.annotations.SerializedName

data class DatabaseCharacterStrokeData(
    val character: String,
    val strokes: List<String>
)

data class DatabaseKanjiData(
    val kanji: String,
    val meanings: List<String>,
    val onReadings: List<String>,
    val kunReadings: List<String>,
    val frequency: Int?,
    val variantFamily: String?
)

enum class DatabaseKanjiReadingType(val value: String) {
    ON("on"),
    KUN("kun")
}

data class DatabaseKanjiClassification(
    val kanji: String,
    val classification: String
)

data class DatabaseKanjiRadical(
    val kanji: String,
    val radical: String,
    val startPosition: Int,
    val strokesCount: Int
)

data class DatabaseFuriganaItem(
    @SerializedName("t") val text: String,
    @SerializedName("a") val annotation: String? = null
)

data class DatabaseRadical(
    val radical: String,
    val strokes: Int
)

data class DatabaseVocabData(
    val entries: List<Vocab_entry>,
    val kanjiElements: List<Vocab_kanji_element>,
    val kanjiInformation: List<Vocab_kanji_information>,
    val kanjiPriorities: List<Vocab_kanji_priority>,
    val kanaElements: List<Vocab_kana_element>,
    val kanaRestrictions: List<Vocab_kana_restriction>,
    val kanaInformation: List<Vocab_kana_information>,
    val kanaPriorities: List<Vocab_kana_priority>,
    val senses: List<Vocab_sense>,
    val senseKanjiRestrictions: List<Vocab_sense_kanji_restriction>,
    val senseReadingRestrictions: List<Vocab_sense_kana_restriction>,
    val sensePartsOfSpeech: List<Vocab_sense_part_of_speech>,
    val senseCrossReferences: List<Vocab_sense_cross_reference>,
    val senseAntonyms: List<Vocab_sense_antonym>,
    val senseFields: List<Vocab_sense_field>,
    val senseMiscellaneous: List<Vocab_sense_miscellaneous>,
    val senseDialects: List<Vocab_sense_dialect>,
    val senseGlosses: List<Vocab_sense_gloss>,
    val senseInformation: List<Vocab_sense_information>,
    val senseExample: List<Vocab_sense_example>,
    val entities: List<Vocab_entity>,
    val furigana: List<Vocab_furigana>
)
