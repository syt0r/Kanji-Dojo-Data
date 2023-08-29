package task

import com.google.gson.Gson
import export.json.JsonCharacterData
import export.json.JsonExporter
import export.json.JsonExpressionData
import parser.SvgCommandParser

fun main() {

    val kanjiTranslations = mutableListOf<String>()
    val expressionTranslations = mutableListOf<String>()

    val verboseErrors = mutableListOf<String>()
    val criticalErrors = mutableListOf<String>()

    val gson = Gson()

    JsonExporter.charactersDir.listFiles()?.forEach {
        val character = it.nameWithoutExtension.first()
        val characterData = JsonCharacterData.readFromFile(it, gson)

        if (characterData.strokes.isEmpty()) {
            criticalErrors.add("$character no strokes")
        }

        characterData.strokes.forEachIndexed { index, path ->
            runCatching {
                SvgCommandParser.parse(path)
            }.getOrElse {
                criticalErrors.add("$character stroke $index can't be parsed")
            }
        }

        when (characterData) {
            is JsonCharacterData.Kana -> {

            }

            is JsonCharacterData.Kanji -> {

                characterData.meanings.forEach { kanjiTranslations.add(it.locale) }

                if (characterData.run { meanings.isEmpty() })
                    verboseErrors.add("$character kanji has no meanings")

                if (characterData.run { kunReadings.isEmpty() && onReadings.isEmpty() })
                    verboseErrors.add("$character kanji has no readings")

            }
        }

    } ?: throw IllegalStateException("No characters found")


    JsonExporter.expressionsDir.listFiles()?.forEach { file ->

        val expressionId = file.nameWithoutExtension
        val expressionData = gson.fromJson(file.readText(), JsonExpressionData::class.java)

        expressionData.readings.forEach {
            if (it.kanjiExpression != null && it.furiganaExpression.isEmpty()) {
                criticalErrors.add("Expression $expressionId has kanji reading ${it.kanjiExpression} without furigana")
            }
        }

        expressionData.readings.flatMap { it.furiganaExpression }
            .filter { it.annotation != null && it.text.length > 1 }
            .forEach {
                verboseErrors.add("Expression $expressionId has furigana part ${it.text} that covers more than one kanji")
            }

        expressionData.meanings.forEach { expressionTranslations.add(it.locale) }
    } ?: throw IllegalStateException("No expressions found")


    val languageToCharacterTranslations = kanjiTranslations.groupBy { it }
        .toList()
        .sortedByDescending { it.second.size }

    println("Available languages for characters: ${languageToCharacterTranslations.size} ${languageToCharacterTranslations.map { it.first }}")
    languageToCharacterTranslations.forEach {
        println("Language ${it.first} has ${it.second.size} expressions")
    }
    println()

    val languageToExpressionTranslations = expressionTranslations.groupBy { it }
        .toList()
        .sortedByDescending { it.second.size }

    println("Available languages for expressions: ${languageToExpressionTranslations.size} ${languageToExpressionTranslations.map { it.first }}")
    languageToExpressionTranslations.forEach {
        println("Language ${it.first} has ${it.second.size} characters")
    }
    println()

    println("Verbose errors found: ${verboseErrors.size}")
    verboseErrors.forEach {
        println(it)
    }

    if (criticalErrors.isNotEmpty())
        throw IllegalStateException(criticalErrors.joinToString("\n"))

}