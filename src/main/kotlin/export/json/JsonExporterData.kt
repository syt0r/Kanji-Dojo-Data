package export.json

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import isKana
import java.io.File
import java.util.Collections.emptyList

sealed class JsonCharacterData {

    companion object {
        fun readFromFile(file: File, gson: Gson): JsonCharacterData {
            val character = file.nameWithoutExtension.first()
            return gson.fromJson(
                file.readText(),
                if (character.isKana()) Kana::class.java
                else Kanji::class.java,
            )
        }
    }

    abstract val value: String
    abstract val strokes: List<String>

    data class Kana(
        override val value: String,
        override val strokes: List<String>
    ) : JsonCharacterData()

    data class Kanji(
        override val value: String,
        override val strokes: List<String>,
        @SerializedName("kun") val kunReadings: List<String>?,
        @SerializedName("on") val onReadings: List<String>?,
        val frequency: Int?,
        val meanings: List<LocalizedJsonStrings>?,
        val variantsFamily: String?,
        val radicals: List<JsonKanjiRadicalData>?
    ) : JsonCharacterData()

}

data class JsonKanjiRadicalData(
    val radical: String,
    val startStroke: Int,
    val strokes: Int,
    val variant: Boolean? = null,
    val part: Int? = null
)

data class JsonExpressionData(
    val id: String,
    val readings: List<ExpressionReading>,
    val meanings: List<LocalizedJsonStrings>
)

data class LocalizedJsonStrings(
    @SerializedName("lang") val locale: String,
    @SerializedName("values") val values: List<String>
)

private fun mergeLocalizedStrings(
    first: List<LocalizedJsonStrings>?,
    second: List<LocalizedJsonStrings>?
): List<LocalizedJsonStrings>? {
    return (first ?: emptyList()).plus(second ?: emptyList())
        .groupBy { it.locale }
        .map { (locale, strings) ->
            LocalizedJsonStrings(
                locale = locale,
                values = strings.flatMap { it.values }.distinct()
            )
        }
        .takeIf { it.isNotEmpty() }
}

data class ExpressionReading(
    val kanjiExpression: String?,
    val kanaExpression: String?,
    val furiganaExpression: List<FuriganaElement>?,
    val rank: Int?
)

data class FuriganaElement(
    val text: String,
    val annotation: String?
)


data class JsonVocabDeckItem(
    val readings: List<String>,
    val meanings: List<String>,
    val id: List<Long>? = null,
)