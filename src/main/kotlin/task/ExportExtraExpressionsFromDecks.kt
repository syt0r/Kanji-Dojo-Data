package task

import FuriganaProvider
import JmdictExpressionConverter
import ProjectData
import export.json.JsonExporter
import parser.JMdictParser


fun main(args: Array<String>) {
    val jlptDeckWordIdSet = ProjectData.yomichanJlptVocabDir
        .listFiles()!!
        .asSequence()
        .flatMap {
            it.readLines().drop(1).mapNotNull {
                it.split(",").firstOrNull()?.takeIf { it.isNotEmpty() }
            }
        }
        .toSet()

    val jmdictItems = JMdictParser.Instance.parse(ProjectData.jMdictFile)

    val existingExpressionIds = ProjectData.exportExpressionsDir.listFiles()!!
        .map { it.nameWithoutExtension }
        .toSet()

    val extraExpressionsIds = jlptDeckWordIdSet.minus(existingExpressionIds)

    println("Adding ${extraExpressionsIds.size} expressions")

    val jmdictIdToItem = jmdictItems.associateBy { it.entrySequence }

    val furiganaProvider = FuriganaProvider()
    val expressions = extraExpressionsIds.map { id ->
        JmdictExpressionConverter.convert(
            jMdictItem = jmdictIdToItem.getValue(id),
            furiganaProvider = furiganaProvider
        )
    }

    JsonExporter.exportExpressions(expressions = expressions)

}