package task

private val wordIdToRemove = setOf<Long>()
private val wordIdToAdd = setOf<Long>()

fun main() {

    val editedData = ProjectData.supportedVocab
        .readLines()
        .map { it.toLong() }
        .toSet()
        .plus(wordIdToAdd)
        .minus(wordIdToRemove)
        .toList()
        .sorted()
        .joinToString("\n")

    ProjectData.supportedVocab.writeText(editedData)

}