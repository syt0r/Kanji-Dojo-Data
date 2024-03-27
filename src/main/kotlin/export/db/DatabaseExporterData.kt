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

data class DatabaseKanjiRadical(
    val kanji: String,
    val radical: String,
    val startPosition: Int,
    val strokesCount: Int
)

data class DatabaseExpression(
    val id: Long,
    val readings: List<DatabaseExpressionReading>,
    val meanings: List<String>
)

data class DatabaseExpressionReading(
    val kanjiReading: String?,
    val kanaReading: String?,
    val furigana: List<DatabaseFuriganaItem>?,
    val rank: Int
)

data class DatabaseFuriganaItem(
    @SerializedName("t") val text: String,
    @SerializedName("a") val annotation: String? = null
)

data class DatabaseRadical(
    val radical: String,
    val strokes: Int
)
