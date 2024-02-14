package task

import ProjectData
import export.json.JsonCharacterData
import export.json.JsonExporter
import parser.KanjiDicCharacterEncodingData
import parser.KanjiDicParser

fun main() {

    val entries = KanjiDicParser.Instance.parse(ProjectData.kanjiDicFile)

    val variantFamilies = mutableListOf<VariantFamily>()

    entries.forEach { entry ->

        val variantFamily = variantFamilies.find { family ->
            entry.alternativeEncodings.intersect(family.alternativeEncodings).isNotEmpty()
        }

        val variantsEncodingsFromCurrentEntry = entry.alternativeEncodings + entry.variants

        if (variantFamily != null) {
            variantFamily.characterVariants.add(entry.character)
            variantFamily.alternativeEncodings.addAll(variantsEncodingsFromCurrentEntry)
        } else {
            variantFamilies.add(
                VariantFamily(
                    characterVariants = mutableSetOf(entry.character),
                    alternativeEncodings = mutableSetOf(*variantsEncodingsFromCurrentEntry.toTypedArray())
                )
            )
        }

    }

    variantFamilies.removeAll { it.characterVariants.size == 1 }

    println("Variant families found: ${variantFamilies.size}")

    JsonExporter.updateCharacters {
        val kanji = this as? JsonCharacterData.Kanji ?: return@updateCharacters this

        val family = variantFamilies.find { it.characterVariants.contains(kanji.value) }

        if (family != null) {
            kanji.copy(variantsFamily = family.characterVariants.sorted().joinToString(""))
        } else {
            kanji
        }
    }

}

private data class VariantFamily(
    val characterVariants: MutableSet<String>,
    val alternativeEncodings: MutableSet<KanjiDicCharacterEncodingData>
)
