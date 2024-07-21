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

    fun exportExpressions(expressions: List<JsonExpressionData>) {
        ProjectData.exportExpressionsDir.mkdirs()

        expressions.forEach {
            try {
                val file = File(ProjectData.exportExpressionsDir, "${it.id}.json")
                val json = gson.toJson(it)
                file.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateExpressions(block: JsonExpressionData.() -> JsonExpressionData) {
        ProjectData.exportExpressionsDir.listFiles()!!.forEach {
            val data = gson.fromJson(it.bufferedReader(), JsonExpressionData::class.java)
            val updatedData = data.block()
            if (updatedData != data)
                it.writeText(gson.toJson(updatedData))
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

    fun exportVocabDeck(title: String, items: List<JsonVocabDeckItem>) {
        ProjectData.exportVocabDecksDir.mkdirs()

        try {
            val file = File(ProjectData.exportVocabDecksDir, "$title.json")
            val json = gson.toJson(items)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}