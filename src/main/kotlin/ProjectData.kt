import java.io.File

object ProjectData {

    val parserDataDir = File("parser_data/")
    val kanjiVGDir = File(parserDataDir, "kanjivg/kanji/")
    val kanjiDicFile = File(parserDataDir, "kanjidic2.xml")
    val radkFile = File(parserDataDir, "radkfile")
    val jMdictFile = File(parserDataDir, "JMdict")
    val jMdictWithExamplesFile = File(parserDataDir, "JMdict_e_examp")
    val furiganaFile = File(parserDataDir, "JmdictFurigana.json")
    val leedsFrequencyFile = File(parserDataDir, "internet-jp.num")
    val yomichanJlptVocabDir = File(parserDataDir, "yomichan-jlpt-vocab/")

    val exportDir = File("data")
    val exportCharactersDir = File(exportDir, "characters")
    val exportExpressionsDir = File(exportDir, "expressions")
    val exportLetterDecksDir = File(exportDir, "letter_decks")
    val exportVocabDecksDir = File(exportDir, "vocab_decks")

}