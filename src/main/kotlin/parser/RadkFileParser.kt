package parser

import java.io.File
import java.nio.charset.Charset

data class RadkRadicalData(
    val radical: String,
    val strokes: Int,
    val extraData: String?
)

class RadkFileParser {

    fun parse(file: File): List<RadkRadicalData> {
        val lines = file.readLines(Charset.forName("EUC-JP"))

        val data = lines.asSequence()
            .filter { it.startsWith("$") }
            .map {
                val values = it.split(" ")
                RadkRadicalData(
                    radical = values[1],
                    strokes = values[2].toInt(),
                    extraData = values.getOrNull(3)
                )
            }
            .toList()

        return data
    }

}