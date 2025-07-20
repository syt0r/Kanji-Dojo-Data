package parser

import kotlinx.serialization.json.*
import parser.TextAnalysisNode.PartOfSpeech

fun List<TextAnalysisNode>.getFurigana(): FuriganaString = buildFuriganaString {
    forEach { node ->
        when(node) {
            is TextAnalysisNode.AlternativeGroup -> TODO()
            is TextAnalysisNode.Compound -> TODO()
            is TextAnalysisNode.Error -> TODO()
            is TextAnalysisNode.Text -> TODO()
            is TextAnalysisNode.Word -> TODO()
        }
    }
}

object IchiranParser {

    operator fun invoke(jsonArray: JsonArray): List<TextAnalysisNode> {
        return jsonArray.flatMap { sentencePart -> parseSentencePart(sentencePart) }
    }

    private fun parseSentencePart(sentencePart: JsonElement): List<TextAnalysisNode> {
        return when (sentencePart) {
            is JsonPrimitive -> listOf(
                TextAnalysisNode.Text(sentencePart.content)
            )

            is JsonArray -> sentencePart
                .map { sentencePartBlock -> sentencePartBlock.jsonArray }
                .flatten()
                .filterIsInstance<JsonArray>() // ignore ends with some number
                .map { wordsGroup -> wordsGroup.jsonArray }
                .flatten()
                .map { parseRomajiArrayStruct(it) }

            else -> emptyList<TextAnalysisNode>()
        }
    }

    private fun parseRomajiArrayStruct(word: JsonElement): TextAnalysisNode {
        return runCatching {
            val romaji: String
            val data: JsonObject

            word.jsonArray.let {
                romaji = it[0].jsonPrimitive.content
                data = it[1].jsonObject
            }

            data.romajiStructDataAsNodes()
        }.getOrElse {
            println("Couldn't parse word[$word], reason[$it]")
            TextAnalysisNode.Error(null)
        }
    }

    private fun JsonObject.romajiStructDataAsNodes(): TextAnalysisNode {
        val alternatives = get("alternative")?.jsonArray

        return when {
            alternatives != null -> {
                val alternativeNodes = alternatives.map { it.jsonObject.singleOrCompound() }
                TextAnalysisNode.AlternativeGroup(alternativeNodes)
            }

            else -> singleOrCompound()
        }
    }

    private fun JsonObject.singleOrCompound(): TextAnalysisNode {
        val compound = get("compound")?.jsonArray
        return when {
            compound != null -> {
                val components = getValue("components").jsonArray
                TextAnalysisNode.Compound(
                    childNodeList = components.map { it.jsonObject.asWordNode() }
                )
            }

            else -> {
                asWordNode()
            }
        }
    }

    private fun JsonObject.asWordNode(): TextAnalysisNode = runCatching {

        val sequence = get("seq")?.jsonPrimitive?.long
        val text = get("text")!!.jsonPrimitive.content.cleaned()
        val kana = get("kana")!!.jsonPrimitive.content.cleaned()
        val reading = get("reading")!!.jsonPrimitive.content.cleaned()

        runCatching {

            val cards = mutableListOf<TextAnalysisNode.CardData>()

            get("gloss")?.jsonArray?.forEach { glossRaw ->
                val gloss = glossRaw.jsonObject.asGlossary()
                val card = TextAnalysisNode.CardData(
                    sequence = sequence,
                    reading = getVocabReading(reading),
                    notes = emptyList(),
                    glossary = listOf(gloss.definition),
                    partOfSpeech = gloss.partOfSpeech.toList()
                )
                cards.add(card)
            }

            get("conj")?.jsonArray?.forEach { conjRaw ->
                val conj = conjRaw.jsonObject.asConj()
                val reading = conj.reading ?: conj.via.firstNotNullOf { it.reading }
                val props = conj.props.plus(conj.via.flatMap { it.props })
                val glossaries = conj.glossaries.plus(conj.via.flatMap { it.glossaries })
                val card = TextAnalysisNode.CardData(
                    sequence = sequence,
                    reading = getVocabReading(reading),
                    notes = props.map { it.type },
                    glossary = glossaries.map { it.definition },
                    partOfSpeech = props.mapNotNull { it.partOfSpeech }
                        .plus(glossaries.flatMap { it.partOfSpeech })
                        .distinct()
                )
                cards.add(card)
            }

            get("suffix")?.jsonPrimitive?.content?.let { suffix ->
                val card = TextAnalysisNode.CardData(
                    sequence = sequence,
                    reading = getVocabReading(reading),
                    notes = emptyList(),
                    glossary = listOf(suffix),
                    partOfSpeech = emptyList()
                )
                cards.add(card)
            }

            get("counter")?.jsonObject?.let { counterRaw ->
                val value = counterRaw.getValue("value").jsonPrimitive.content.lowercase()
                val gloss = "Counter, $value"
                val card = TextAnalysisNode.CardData(
                    sequence = sequence,
                    reading = getVocabReading(reading),
                    notes = emptyList(),
                    glossary = listOf(gloss),
                    partOfSpeech = emptyList()
                )
                cards.add(card)
            }

            if (cards.isEmpty()) {
                // Kana only words
                return@runCatching TextAnalysisNode.Text(text)
            }

            TextAnalysisNode.Word(
                sequence = sequence,
                text = getValue("text").jsonPrimitive.content.cleaned(),
                reading = VocabReading(
                    kanjiReading = text.takeIf { it != kana },
                    kanaReading = kana,
                    furigana = getFurigana(text, kana),
                ),
                cards = cards,
                highlightPartOfSpeech = cards
                    .firstNotNullOfOrNull { it.partOfSpeech.takeIf { it.isNotEmpty() } }
                    ?.minByOrNull { it.ordinal }
            )
        }.getOrElse {
            it.printStackTrace()
            println("parsingError[$it]")
            TextAnalysisNode.Error(text)
        }

    }.getOrElse {
        it.printStackTrace()
        println("parsingError[$it]")
        TextAnalysisNode.Error(null)
    }

    private fun getVocabReading(text: String): VocabReading {
        val formattedKanaReadingStart = text.indexOf("【")
        return when {
            formattedKanaReadingStart == -1 -> VocabReading(
                kanjiReading = null,
                kanaReading = text,
                furigana = null
            )

            else -> {
                val kanji = text.substring(
                    startIndex = 0,
                    endIndex = formattedKanaReadingStart
                ).trim()

                val kana = text.substring(
                    startIndex = formattedKanaReadingStart.plus(1),
                    endIndex = text.indexOf("】")
                )

                VocabReading(
                    kanjiReading = kanji,
                    kanaReading = kana,
                    furigana = getFurigana(kanji, kana)
                )
            }
        }
    }

    private fun getFurigana(text: String, kana: String): FuriganaString {
        return when {
            text == kana -> buildFuriganaString { append(text) }
            else -> buildFuriganaString {
                val commonPrefix = text.commonPrefixWith(kana)
                val commonSuffix = text.commonSuffixWith(kana)

                val middleKanji = text.substring(
                    commonPrefix.length,
                    text.length - commonSuffix.length
                )

                val middleKana = kana.substring(
                    commonPrefix.length,
                    kana.length - commonSuffix.length
                )

                if (commonPrefix.isNotEmpty()) append(commonPrefix)
                append(middleKanji, middleKana)
                if (commonSuffix.isNotEmpty()) append(commonSuffix)
            }
        }
    }

    private fun JsonObject.asGlossary(): Glossary {
        return Glossary(
            definition = getValue("gloss").jsonPrimitive.content,
            partOfSpeech = getValue("pos").jsonPrimitive.content
                .removeSurrounding("[", "]")
                .split(",")
                .mapNotNull { value -> PartOfSpeech.entries.find { it.regex.matches(value) } }
                .toSet()
        )
    }

    private fun JsonObject.asProp(): ConjugationProp {
        val posRaw = getValue("pos").jsonPrimitive.content
        val pos = PartOfSpeech.entries.firstOrNull { it.regex.matches(posRaw) }
        if (pos == null) println("No matching pos found for pos[$posRaw]")
        val type = getValue("type").jsonPrimitive.content
        return ConjugationProp(
            partOfSpeech = pos,
            type = type
        )
    }

    private fun JsonObject.asConj(): Conjugation {
        return Conjugation(
            glossaries = get("gloss")?.jsonArray?.map { it.jsonObject.asGlossary() } ?: emptyList(),
            props = get("prop")?.jsonArray?.map { it.jsonObject.asProp() } ?: emptyList(),
            reading = get("reading")?.jsonPrimitive?.content,
            via = get("via")?.jsonArray?.map { it.jsonObject.asConj() } ?: emptyList()
        )
    }

    private val cleanupRegex = Regex("[\\s\\u200B-\\u200D\\uFEFF\\u2060\\u00AD\\p{Cf}]")
    private fun String.cleaned(): String = trim().replace(cleanupRegex, "")

    private data class Glossary(
        val definition: String,
        val partOfSpeech: Set<PartOfSpeech> = emptySet()
    )

    private data class Conjugation(
        val glossaries: List<Glossary>,
        val props: List<ConjugationProp>,
        val reading: String?,
        val via: List<Conjugation>
    )

    private data class ConjugationProp(
        val partOfSpeech: PartOfSpeech?,
        val type: String
    )

}