package export.json

import ProjectData
import com.google.gson.GsonBuilder
import java.io.File


object JsonExporter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun exportCharacters(characters: List<JsonCharacterData>) {
        ProjectData.exportCharactersDir.mkdirs()

        characters.forEach {
            val file = File(ProjectData.exportCharactersDir, "${it.value}.json")
            writeCharacterData(file, it)
        }
    }

    fun updateCharacters(block: JsonCharacterData.() -> JsonCharacterData) {
        ProjectData.exportCharactersDir.listFiles()!!.forEach {
            val characterData = JsonCharacterData.readFromFile(it, gson)
            val updatedCharacterData = characterData.block()
            if (updatedCharacterData != characterData)
                writeCharacterData(it, updatedCharacterData)
        }
    }

    fun exportExpressions(
        expressions: List<JsonExpressionData>,
        mergeExistingData: Boolean = true
    ) {
        ProjectData.exportExpressionsDir.mkdirs()

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

    private fun writeCharacterData(file: File, characterData: JsonCharacterData) {
        try {
            val json = gson.toJson(characterData)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}