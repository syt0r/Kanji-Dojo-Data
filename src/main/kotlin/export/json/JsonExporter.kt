package export.json

import com.google.gson.GsonBuilder
import java.io.File


object JsonExporter {

    private val exportDir = File("data")
    val charactersDir = File(exportDir, "characters")
    val expressionsDir = File(exportDir, "expressions")

    fun exportCharacters(
        characters: List<JsonCharacterData>,
        mergeExistingData: Boolean = true
    ) {
        charactersDir.mkdirs()

        val gson = GsonBuilder().setPrettyPrinting().create()
        characters.forEach {
            try {
                val file = File(charactersDir, "${it.value}.json")

                val json = if (mergeExistingData && file.exists()) {
                    val existing: JsonCharacterData = JsonCharacterData.readFromFile(file, gson)
                    val merged = existing.mergeWith(it)
                    gson.toJson(merged)
                } else {
                    gson.toJson(it)
                }

                file.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportExpressions(
        expressions: List<JsonExpressionData>,
        mergeExistingData: Boolean = true
    ) {
        expressionsDir.mkdirs()

        val gson = GsonBuilder().setPrettyPrinting().create()
        expressions.forEach {
            try {
                val file = File(expressionsDir, "${it.id}.json")

                val json = if (mergeExistingData && file.exists()) {
                    val existing: JsonExpressionData = gson.fromJson(file.readText(), JsonExpressionData::class.java)
                    val merged = existing.mergeWith(it)
                    gson.toJson(merged)
                } else {
                    gson.toJson(it)
                }

                file.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}