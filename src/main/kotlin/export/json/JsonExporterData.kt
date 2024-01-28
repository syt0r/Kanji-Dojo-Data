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
                if (character.isKana()) JsonCharacterData.Kana::class.java
                else JsonCharacterData.Kanji::class.java
            )
        }
    }

    abstract val value: String
    abstract val strokes: List<String>

    abstract fun mergeWith(other: JsonCharacterData): JsonCharacterData

    data class Kana(
        override val value: String,
        override val strokes: List<String> = emptyList()
    ) : JsonCharacterData() {

        override fun mergeWith(other: JsonCharacterData): JsonCharacterData {
            if (other !is Kana) throw IllegalArgumentException("Can't merge kana with kanji for [$value]")
            return Kana(
                value = value,
                strokes = strokes.takeIf { it.isNotEmpty() } ?: other.strokes
            )
        }

    }

    data class Kanji(
        override val value: String,
        override val strokes: List<String> = emptyList(),
        @SerializedName("kun") val kunReadings: List<String> = emptyList(),
        @SerializedName("on") val onReadings: List<String> = emptyList(),
        val frequency: Int? = null,
        val meanings: List<LocalizedJsonStrings> = emptyList(),
        val radicals: List<JsonKanjiRadicalData> = emptyList()
    ) : JsonCharacterData() {

        override fun mergeWith(other: JsonCharacterData): JsonCharacterData {
            if (other !is Kanji) throw IllegalArgumentException("Can't merge kana with kanji for [$value]")
            return Kanji(
                value = value,
                frequency = frequency ?: other.frequency,
                kunReadings = kunReadings.plus(other.kunReadings).distinct(),
                onReadings = onReadings.plus(other.onReadings).distinct(),
                meanings = meanings.mergeWith(other.meanings),
                strokes = strokes.takeIf { it.isNotEmpty() } ?: other.strokes,
                radicals = radicals.plus(other.radicals)
                    .groupBy { it.radical to it.startStroke }
                    .map { (radicalToStartStroke, variants) ->
                        variants.first().copy(
                            variant = variants.firstNotNullOfOrNull { it.variant },
                            part = variants.firstNotNullOfOrNull { it.part }
                        )
                    }
                    .toList()
                    .sortedWith(compareBy({ it.startStroke }, { it.radical }))
            )
        }

    }

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
) {

    fun mergeWith(other: JsonExpressionData): JsonExpressionData {
        return JsonExpressionData(
            id = id,
            readings = readings.plus(other.readings).distinct(),
            meanings = meanings.mergeWith(other.meanings)
        )
    }

}

data class LocalizedJsonStrings(
    @SerializedName("lang") val locale: String,
    @SerializedName("values") val values: List<String>
)

private fun List<LocalizedJsonStrings>.mergeWith(other: List<LocalizedJsonStrings>) = map {
    val locale = it.locale
    LocalizedJsonStrings(
        locale = it.locale,
        values = it.values.plus(other.find { it.locale == locale }?.values ?: emptyList()).distinct()
    )
}

data class ExpressionReading(
    val kanjiExpression: String? = null,
    val kanaExpression: String,
    val furiganaExpression: List<FuriganaElement> = emptyList(),
    val ranking: Int?
)

data class FuriganaElement(
    val text: String,
    val annotation: String?
)
