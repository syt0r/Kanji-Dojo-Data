package task

import export.json.JsonExporter
import parser.RadkFileParser
import java.io.File

fun main() {
    val radkFile = File("parser_data/radkfile")
    val data = RadkFileParser().parse(radkFile)
    val characters = JsonExporter.charactersDir.listFiles()!!.map { it.nameWithoutExtension }.toSet()
    val radicals = data.map { it.radical }.toSet()
    println("Diff ${radicals.minus(characters)}")
}