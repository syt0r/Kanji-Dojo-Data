import java.io.File

object ProjectData {

    val parserDataDir = File("parser_data/")

    val kanjiVGDir = File(parserDataDir, "kanjivg/kanji/")
    val kanjiDicFile = File(parserDataDir, "kanjidic2.xml")
    val radkFile = File("parser_data/radkfile")

    val jMdictFile = File(parserDataDir, "JMdict")
    val furiganaFile = File(parserDataDir, "JmdictFurigana.json")

    val exportDir = File("data")
    val exportCharactersDir = File(exportDir, "characters")
    val exportExpressionsDir = File(exportDir, "expressions")

}