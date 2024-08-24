package task

import ProjectData
import com.google.gson.Gson
import com.google.gson.JsonObject
import export.json.JsonExpressionData
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking<Unit> {
    val language = "Ukrainian"
    val languageCode = "ua"

    val outputFile = File(ProjectData.exportDir, "translations_$languageCode.csv")

    val words = ProjectData.exportExpressionsDir.listFiles()!!
        .map { Gson().fromJson(it.readText(), JsonExpressionData::class.java) }
        .associateBy { it.id }

    val existingTranslatedWordIds = outputFile.takeIf { it.exists() }
        ?.readLines()
        ?.map { it.split(",").first() }
        ?.toSet()
        ?: emptySet()

    val wordsToTranslate = words.filterKeys { existingTranslatedWordIds.contains(it).not() }

    println("Total words: ${words.size}")
    println("Translated words: ${existingTranslatedWordIds.size}")
    println("Words to translate: ${wordsToTranslate.size}")

    val client = HttpClient(CIO)

    val writer = outputFile.bufferedWriter()

    wordsToTranslate.forEach { (_, expression) ->

        val readings: List<String> = expression.readings.map { it.kanjiExpression ?: it.kanaExpression!! }
        val meanings: List<String> = expression.meanings.first { it.locale == "en" }.values

        val prompt = getWordTranslationPrompt(readings, meanings, language)
        val response = makeAiRequest(client, prompt)

        val translations = response.split(",")
            .map { it.trim().replace("\\s+".toRegex(), " ") }
            .filter { it.isNotEmpty() }

        println("Exporting field: ${expression.id}, $readings, $translations")

        val outputValues = listOf(expression.id) + translations
        val csvOutput = outputValues.joinToString()

        writer.appendLine(csvOutput)
        writer.flush()

        delay(20000)

    }

}

fun getWordTranslationPrompt(
    japaneseReadings: List<String>,
    englishTranslations: List<String>,
    language: String
): String {
    return """
    You are given a Japanese word with readings $japaneseReadings that means $englishTranslations.
    Generate CSV array of strings with its meanings in $language language with no extra output
    """.trimIndent()
}


private suspend fun makeAiRequest(
    client: HttpClient,
    prompt: String
): String {

    val endpoint = System.getenv("ENDPOINT")
    val projectId = System.getenv("PROJECT_ID")
    val region = System.getenv("REGION")
    val accessToken = System.getenv("ACCESS_TOKEN")

    val requestBody = mapOf(
        "model" to "meta/llama3-405b-instruct-maas",
        "stream" to false,
        "messages" to listOf(mapOf("role" to "user", "content" to prompt))
    )

    val response = client.post(
        "https://$endpoint/v1beta1/projects/$projectId/locations/$region/endpoints/openapi/chat/completions"
    ) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $accessToken")
            append(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        setBody(Gson().toJson(requestBody))
    }
    println("Request Response: $response")

    val jsonResponse = Gson().fromJson(response.bodyAsText(), JsonObject::class.java)
    println("Request Body: $jsonResponse")

    val responseMessage = jsonResponse.getAsJsonArray("choices")
        .first().asJsonObject
        .get("message").asJsonObject
        .get("content").asString
    println("Request Response Message: $responseMessage")

    return responseMessage
}