package task

import ProjectData
import com.google.gson.Gson
import export.json.JsonExpressionData

private val input = """
    
""".trimIndent()

fun main() {
    val gson = Gson()

    val lookupExpressionReadings = input.split(",").map { it.trim() }

    val expressions = ProjectData.exportExpressionsDir.listFiles()!!.map {
        gson.fromJson(it.readText(), JsonExpressionData::class.java)
    }

    val readingToExpressions = lookupExpressionReadings.map { lookupReading ->
        println("Looking for: $lookupReading")
        val foundExpressions = expressions.filter {
            it.readings.any { it.kanaExpression == lookupReading || it.kanjiExpression == lookupReading }
        }
        println("Found ${foundExpressions.size} entries: ${foundExpressions.joinToString { it.id }}")
        lookupReading to foundExpressions
    }

    println("Output:" + readingToExpressions.flatMap { it.second }.map { it.id })

}