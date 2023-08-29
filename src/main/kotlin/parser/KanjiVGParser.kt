package parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

interface KanjiVgParser {

    companion object {
        val Instance: KanjiVgParser = DefaultKanjiVGParser
    }

    fun parse(kanjiVGDataFolder: File): List<CharacterWritingData>

}

data class CharacterWritingData(
    val character: Char,
    val strokes: List<String>,
    val standardRadicals: List<Radical>,
    val allRadicals: List<CharacterRadical>
)

data class Radical(
    val radical: String,
    val strokeCount: Int
)

data class CharacterRadical(
    val character: String,
    val radical: String,
    val startPosition: Int,
    val strokesCount: Int
)

private data class KanjiVGCharacterData(
    val character: Char,
    val compound: KanjiVGCompound
)

private sealed class KanjiVGCompound {

    data class Group(
        val element: String?,
        val position: String?,
        val radical: String?,
        val part: Int?,
        val partial: Boolean?,
        val variant: Boolean?,
        val childrens: List<KanjiVGCompound>,
        val startStrokeIndex: Int,
        val endStrokeIndex: Int
    ) : KanjiVGCompound()

    data class Path(
        val path: String
    ) : KanjiVGCompound()

}

private object DefaultKanjiVGParser : KanjiVgParser {

    override fun parse(kanjiVGDataFolder: File): List<CharacterWritingData> {
        println("Parsing KanjiVG...")

        val kanjiFiles = kanjiVGDataFolder.listFiles()
            ?: throw IllegalStateException("No files found")

        println("${kanjiFiles.size} files found, start parsing")

        return kanjiFiles.asSequence()
            // filters out uncommon character variations (KaishoXXX, Hz, Vt)
            .filter { it.name.contains("-").not() }
            .map { parseSvgFile(it) }
            .groupBy { it.character }
            .map {
                it.value.run {
                    if (size != 1) error("Character variation found for ${it.key}")
                    first()
                }
            }
            .map { KanjiVGConverter.toCharacterWritingData(it) }
            .toList()
    }

    private fun parseSvgFile(file: File): KanjiVGCharacterData {
        val fileName = file.nameWithoutExtension
        val character = Integer.parseInt(fileName.split("-").first(), 16).toChar()

        val document = Jsoup.parse(file, Charsets.UTF_8.name())
        val strokesElement = document.selectFirst("[id*=StrokePath]")!!
        val rootCompound = strokesElement
            .selectFirst("[id=kvg:${file.nameWithoutExtension}]")!!
            .parseCompound()

        return KanjiVGCharacterData(
            character = character,
            compound = rootCompound
        )
    }

    private fun Element.parseCompound(): KanjiVGCompound {
        return when (val t = tagName()) {
            "path" -> KanjiVGCompound.Path(attr("d"))
            "g" -> {

                val childPathIndexes = select("path").asSequence()
                    .map { it.attr("id") }
                    .map {
                        val sIndex = it.indexOfLast { it == 's' }
                        assert(sIndex != -1)
                        it.substring(sIndex + 1)
                    }
                    .map { it.toInt() }
                    .toList()

                KanjiVGCompound.Group(
                    element = attr("kvg:element").takeIf { it.isNotEmpty() },
                    position = attr("kvg:position").takeIf { it.isNotEmpty() },
                    radical = attr("kvg:radical").takeIf { it.isNotEmpty() },
                    part = attr("kvg:part")?.takeIf { it.isNotEmpty() }?.toInt(),
                    partial = attr("kvg:partial")?.toBooleanStrictOrNull(),
                    variant = attr("kvg:variant")?.toBooleanStrictOrNull(),
                    childrens = children().map { it.parseCompound() },
                    startStrokeIndex = childPathIndexes.minOrNull()?.minus(1) ?: -1,
                    endStrokeIndex = childPathIndexes.maxOrNull()?.minus(1) ?: -1
                ).apply {
                    if (startStrokeIndex == -1 || endStrokeIndex == -1) {
                        println("Error, empty group ${attr("id")}")
                    }
                }
            }

            else -> throw IllegalStateException("Unknown tag[$t]")
        }
    }

}

private object KanjiVGConverter {

    fun toCharacterWritingData(kanjiVGCharacterData: KanjiVGCharacterData): CharacterWritingData {
        return CharacterWritingData(
            character = kanjiVGCharacterData.character,
            strokes = kanjiVGCharacterData.compound.getPaths()
                .onEach { SvgCommandParser.parse(it) }, // verification,
            standardRadicals = kanjiVGCharacterData.compound.findStandardRadicals(),
            allRadicals = kanjiVGCharacterData.compound.run {
                this as KanjiVGCompound.Group
                if (radical != null) { // Ignores top level element unless radical
                    findAllRadicals(kanjiVGCharacterData.character.toString())
                } else {
                    childrens.flatMap { it.findAllRadicals(kanjiVGCharacterData.character.toString()) }
                }
            }
        )
    }

    private fun KanjiVGCompound.getPaths(): List<String> {
        return when (this) {
            is KanjiVGCompound.Group -> childrens.flatMap { it.getPaths() }
            is KanjiVGCompound.Path -> listOf(path)
        }
    }

    private fun KanjiVGCompound.findStandardRadicals(): List<Radical> {
        return when (this) {
            is KanjiVGCompound.Group -> {
                val isStandardRadical = element != null &&
                        part == null &&
                        partial != true &&
                        variant != true &&
                        radical?.isNotEmpty() == true

                val radicals = if (isStandardRadical) {
                    listOf(
                        Radical(
                            radical = element!!,
                            strokeCount = endStrokeIndex - startStrokeIndex + 1
                        )
                    )
                } else listOf()

                radicals.plus(childrens.flatMap { it.findStandardRadicals() })
            }

            is KanjiVGCompound.Path -> listOf()
        }
    }

    private fun KanjiVGCompound.findAllRadicals(kanji: String): List<CharacterRadical> {
        return when (this) {
            is KanjiVGCompound.Group -> {
                val isRadical = element != null

                val radicals = if (isRadical) {
                    listOf(
                        CharacterRadical(
                            character = kanji,
                            radical = element!!,
                            startPosition = startStrokeIndex,
                            strokesCount = endStrokeIndex - startStrokeIndex + 1
                        )
                    )
                } else listOf<CharacterRadical>()

                radicals.plus(childrens.flatMap { it.findAllRadicals(kanji) })
            }

            is KanjiVGCompound.Path -> listOf()
        }
    }

}
