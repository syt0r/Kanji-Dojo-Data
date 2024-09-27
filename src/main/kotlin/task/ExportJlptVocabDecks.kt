package task

import ProjectData
import java.io.File

fun main(args: Array<String>) {
    val jlptDeckFileNameToWordIdListMap = ProjectData.yomichanJlptVocabDir
        .listFiles()!!
        .asSequence()
        .map {
            it.name to it.readLines()
                .drop(1)
                .mapNotNull { it.split(",").firstOrNull()?.takeIf { it.isNotEmpty() } }
                .distinct()
        }

    jlptDeckFileNameToWordIdListMap.forEach { (fileName, wordIdList) ->
        File(ProjectData.exportVocabDecksDir, fileName).writeText(
            wordIdList.joinToString("\n")
        )
    }
}
