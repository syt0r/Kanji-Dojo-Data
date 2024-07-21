package task

import FuriganaProvider
import JmdictExpressionConverter
import ProjectData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import export.json.JsonExporter
import export.json.JsonVocabDeckItem
import parser.JMdictParser


fun main(args: Array<String>) {
    val json = Gson()
    val deckTypeToken = object : TypeToken<List<JsonVocabDeckItem>>() {}

    val vocabItems = ProjectData.exportVocabDecksDir
        .listFiles()!!
        .asSequence()
        .flatMap { json.fromJson(it.readText(), deckTypeToken) }

    val fullMatchedVocabItems = vocabItems.filter { it.id?.size == 1 }

    val jmdictItems = JMdictParser.Instance.parse(ProjectData.jMdictFile)

    val existingExpressionIds = ProjectData.exportExpressionsDir.listFiles()!!
        .map { it.nameWithoutExtension }
        .toSet()

    val extraExpressionsIds = fullMatchedVocabItems.map { it.id!!.first().toString() }
        .toSet()
        .minus(existingExpressionIds)

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