package export.json

import ProjectData
import com.google.gson.GsonBuilder
import java.io.File


object JsonExporter {

    fun exportCharacters(
        characters: List<JsonCharacterData>,
        mergeExistingData: Boolean = true
    ) {
        ProjectData.exportCharactersDir.mkdirs()

        val gson = GsonBuilder().setPrettyPrinting().create()
        characters.forEach {
            try {
                val file = File(ProjectData.exportCharactersDir, "${it.value}.json")

                val json = if (mergeExistingData && file.exists()) {
                    val existing: JsonCharacterData = JsonCharacterData.readFromFile(file, gson)
                    if (existing is JsonCharacterData.Kanji) {
                        gson.toJson(existing.mergeWith(it))
                    } else {
                        gson.toJson(it)
                    }
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
        ProjectData.exportExpressionsDir.mkdirs()

        val gson = GsonBuilder().setPrettyPrinting().create()
        expressions.forEach {
            try {
                val file = File(ProjectData.exportExpressionsDir, "${it.id}.json")

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