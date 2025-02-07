package parser

import FuriganaProvider
import ProjectData
import com.google.gson.Gson
import export.db.*
import export.json.FuriganaElement
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

private data class DatabaseVocabSingleEntry(
    val entry: Vocab_entry,
    val kanjiElements: MutableList<Vocab_kanji_element> = mutableListOf(),
    val kanjiInformation: MutableList<Vocab_kanji_information> = mutableListOf(),
    val kanjiPriorities: MutableList<Vocab_kanji_priority> = mutableListOf(),
    val readingElements: MutableList<Vocab_kana_element> = mutableListOf(),
    val readingRestrictions: MutableList<Vocab_kana_restriction> = mutableListOf(),
    val readingInformation: MutableList<Vocab_kana_information> = mutableListOf(),
    val readingPriorities: MutableList<Vocab_kana_priority> = mutableListOf(),
    val senses: MutableList<Vocab_sense> = mutableListOf(),
    val senseKanjiRestrictions: MutableList<Vocab_sense_kanji_restriction> = mutableListOf(),
    val senseReadingRestrictions: MutableList<Vocab_sense_reading_restriction> = mutableListOf(),
    val partsOfSpeech: MutableList<Vocab_sense_part_of_speech> = mutableListOf(),
    val crossReferences: MutableList<Vocab_sense_cross_reference> = mutableListOf(),
    val antonyms: MutableList<Vocab_sense_antonym> = mutableListOf(),
    val fields: MutableList<Vocab_sense_field> = mutableListOf(),
    val miscellaneous: MutableList<Vocab_sense_miscellaneous> = mutableListOf(),
    val dialects: MutableList<Vocab_sense_dialect> = mutableListOf(),
    val glosses: MutableList<Vocab_sense_gloss> = mutableListOf(),
    val senseInformation: MutableList<Vocab_sense_information> = mutableListOf(),
    val senseExample: MutableList<Vocab_sense_example> = mutableListOf(),
    val furigana: MutableList<Vocab_furigana> = mutableListOf()
)

object CompositeJMdictParser {

    fun parse(
        wordsPool: Set<Long>,
        file: File = ProjectData.jMdictWithExamplesFile,
    ): DatabaseVocabData {
        val idGenerator = IdGenerator()
        val furiganaProvider = FuriganaProvider()
        val gson = Gson()

        return Jsoup.parse(file, Charsets.UTF_8.name())
            .select("entry")
            .mapNotNull {
                val entryId = it.selectFirst("ent_seq")!!.text().toLong()
                if (!wordsPool.contains(entryId)) return@mapNotNull null

                parseEntry(it, idGenerator, furiganaProvider, gson)
            }
            .run {
                DatabaseVocabData(
                    entries = map { it.entry },
                    kanjiElements = flatMap { it.kanjiElements },
                    kanjiInformation = flatMap { it.kanjiInformation },
                    kanjiPriorities = flatMap { it.kanjiPriorities },
                    readingElements = flatMap { it.readingElements },
                    readingRestrictions = flatMap { it.readingRestrictions },
                    readingInformation = flatMap { it.readingInformation },
                    readingPriorities = flatMap { it.readingPriorities },
                    senses = flatMap { it.senses },
                    senseKanjiRestrictions = flatMap { it.senseKanjiRestrictions },
                    senseReadingRestrictions = flatMap { it.senseReadingRestrictions },
                    partsOfSpeech = flatMap { it.partsOfSpeech },
                    crossReferences = flatMap { it.crossReferences },
                    antonyms = flatMap { it.antonyms },
                    fields = flatMap { it.fields },
                    miscellaneous = flatMap { it.miscellaneous },
                    dialects = flatMap { it.dialects },
                    glosses = flatMap { it.glosses }
                        .filter { it.language == null } // filter only English
                        .distinct(), // distinct cause some entries contain badly formatted glosses
                    senseInformation = flatMap { it.senseInformation },
                    senseExample = flatMap { it.senseExample },
                    furigana = flatMap { it.furigana }.distinct()
                )
            }
    }

    private fun parseEntry(
        entryElement: Element,
        idGenerator: IdGenerator,
        furiganaProvider: FuriganaProvider,
        gson: Gson
    ): DatabaseVocabSingleEntry {
        val entryId = entryElement.selectFirst("ent_seq")!!.text().toLong()

        val dbEntry = DatabaseVocabSingleEntry(
            entry = Vocab_entry(entryId)
        )

        fun String.removeDataSurroundings() = removePrefix("&").removeSuffix(";")

        entryElement.select("k_ele").forEach {
            val elementId = idGenerator.nextId("k_ele")
            val reading = it.selectFirst("keb")!!.text()
            dbEntry.kanjiElements.add(Vocab_kanji_element(elementId, entryId, reading))

            it.select("ke_inf").forEach {
                val infoValue = it.text().removeDataSurroundings()
                dbEntry.kanjiInformation.add(Vocab_kanji_information(elementId, infoValue))
            }

            it.select("ke_pri").forEach {
                val value = it.text()
                dbEntry.kanjiPriorities.add(Vocab_kanji_priority(elementId, value))
            }
        }

        entryElement.select("r_ele").forEach {
            val elementId = idGenerator.nextId("r_ele")
            val reading = it.selectFirst("reb")!!.text()
            val noKanji = it.selectFirst("re_nokanji")?.let { 1L } ?: 0L

            dbEntry.readingElements.add(Vocab_kana_element(elementId, entryId, reading, noKanji))

            it.select("re_restr").forEach {
                val kanjiReading = it.text()
                dbEntry.readingRestrictions.add(Vocab_kana_restriction(elementId, kanjiReading))
            }

            it.select("re_inf").forEach {
                val information = it.text().removeDataSurroundings()
                dbEntry.readingInformation.add(Vocab_kana_information(elementId, information))
            }

            it.select("re_pri").forEach {
                val value = it.text()
                dbEntry.readingPriorities.add(Vocab_kana_priority(elementId, value))
            }
        }

        entryElement.select("sense").forEach {
            val senseId = idGenerator.nextId("sense")
            dbEntry.senses.add(Vocab_sense(senseId, entryId))

            it.select("stagk").forEach {
                dbEntry.senseKanjiRestrictions.add(Vocab_sense_kanji_restriction(senseId, it.text()))
            }

            it.select("stagr").forEach {
                dbEntry.senseReadingRestrictions.add(Vocab_sense_reading_restriction(senseId, it.text()))
            }

            it.select("xref").forEach {
                dbEntry.crossReferences.add(Vocab_sense_cross_reference(senseId, it.text()))
            }

            it.select("ant").forEach {
                dbEntry.antonyms.add(Vocab_sense_antonym(senseId, it.text()))
            }

            it.select("pos").forEach {
                dbEntry.partsOfSpeech.add(Vocab_sense_part_of_speech(senseId, it.text().removeDataSurroundings()))
            }

            it.select("field").forEach {
                dbEntry.fields.add(Vocab_sense_field(senseId, it.text().removeDataSurroundings()))
            }

            it.select("misc").forEach {
                dbEntry.miscellaneous.add(Vocab_sense_miscellaneous(senseId, it.text().removeDataSurroundings()))
            }

            it.select("dial").forEach {
                dbEntry.dialects.add(Vocab_sense_dialect(senseId, it.text().removeDataSurroundings()))
            }

            it.select("gloss").forEach {
                dbEntry.glosses.add(Vocab_sense_gloss(senseId, it.text(), it.attr("xml:lang"), it.attr("g_type")))
            }

            it.select("s_inf").forEach {
                dbEntry.senseInformation.add(Vocab_sense_information(senseId, it.text()))
            }

            it.select("example").forEach {
                val text = it.selectFirst("ex_text")!!.text()
                val sentences = it.select("ex_sent")
                val japaneseSentence = sentences.first { it.attr("xml:lang") == "jpn" }.text()
                val translation = sentences.first { it.attr("xml:lang") == "eng" }.text()
                dbEntry.senseExample.add(Vocab_sense_example(senseId, text, japaneseSentence, translation))
            }
        }

        dbEntry.kanjiElements.forEach { vocabKanjiElement ->
            val kanjiReading = vocabKanjiElement.reading
            val kanaRestrictions = dbEntry.readingRestrictions.filter { it.restricted_kanji == kanjiReading }

            val kanjiReadingToKanaReadingPairs = if (kanaRestrictions.isEmpty()) {
                dbEntry.readingElements.map { kanjiReading to it.reading }
            } else {
                kanaRestrictions.map { kanaRestriction ->
                    val kanaElement = dbEntry.readingElements.first { it.element_id == kanaRestriction.element_id }
                    kanjiReading to kanaElement.reading
                }
            }

            kanjiReadingToKanaReadingPairs.forEach { (kanji, kana) ->
                val furigana = furiganaProvider.getFuriganaForReading(kanji, kana)
                    ?.map { it.toDbEntity() }
                    ?.let { gson.toJson(it) }
                if (furigana != null) {
                    dbEntry.furigana.add(Vocab_furigana(kanji, kana, furigana))
                }
            }
        }

        return dbEntry
    }

}

fun FuriganaElement.toDbEntity() = DatabaseFuriganaItem(text, annotation)

class IdGenerator {

    private val idMap = mutableMapOf<String, Long>()

    fun nextId(key: String): Long {
        val current = idMap[key] ?: 0
        return (current + 1).also { idMap[key] = it }
    }

}