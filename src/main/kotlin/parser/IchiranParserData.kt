package parser

data class FuriganaString(
    val compounds: List<FuriganaStringCompound>
) {

    operator fun plus(string: String): FuriganaString {
        return FuriganaString(compounds.plus(FuriganaStringCompound(string)))
    }

}

data class FuriganaStringCompound(
    val text: String,
    val annotation: String? = null
)

class FuriganaStringBuilder {

    private val list = mutableListOf<FuriganaStringCompound>()

    fun append(character: String, annotation: String? = null) =
        list.add(FuriganaStringCompound(character, annotation))

    fun append(furiganaString: FuriganaString) {
        list.addAll(furiganaString.compounds)
    }

    fun build() = FuriganaString(list)

}

fun buildFuriganaString(scope: FuriganaStringBuilder.() -> Unit): FuriganaString {
    val builder = FuriganaStringBuilder()
    builder.scope()
    return builder.build()
}

data class VocabReading(
    val kanjiReading: String?,
    val kanaReading: String,
    val furigana: FuriganaString?
)

sealed interface TextAnalysisNode {

    data class Text(
        val value: String
    ) : TextAnalysisNode

    data class Word(
        val sequence: Long?,
        val text: String,
        val reading: VocabReading,
        val cards: List<CardData>,
        val highlightPartOfSpeech: PartOfSpeech?
    ) : TextAnalysisNode

    data class Compound(
        val childNodeList: List<TextAnalysisNode>
    ) : TextAnalysisNode

    data class Error(
        val text: String?
    ) : TextAnalysisNode

    data class AlternativeGroup(
        val childNodeList: List<TextAnalysisNode>
    ) : TextAnalysisNode

    data class CardData(
        val sequence: Long?,
        val reading: VocabReading,
        val notes: List<String>,
        val glossary: List<String>,
        val partOfSpeech: List<PartOfSpeech>
    )

    enum class PartOfSpeech(regexPattern: String) {
        Noun("^(n|n-adv|n-pr|n-pref|n-suf|n-t|adj-no)\$"),
        Verb("^v.*"),
        Adjective("^adj.*"),
        Particle("^prt\$"),
        Suffix("^suf\$"),
        Prefix("^pref\$"),
        Expression("^exp\$"),
        Counter("^ctr\$"),
        Interjection("^int\$"),
        Conjunction("^conj\$"),
        Pronoun("^pn\$"),
        Number("^num\$"),
        Adverb("^adv.*"),
        Auxiliary("^aux.*"),
        Copula("^cop\$"),
        Unclassified("^unc\$");

        val regex = Regex(regexPattern)

        companion object {

            fun detect(tag: String): PartOfSpeech? {
                return entries.firstOrNull { it.regex.matches(tag) }
            }

            fun detectAll(tags: List<String>): Set<PartOfSpeech> {
                return tags.mapNotNull { detect(it) }.toSet()
            }

        }
    }

}