import export.json.FuriganaElement
import parser.JMDictFuriganaRubyItem
import parser.JMdictFuriganaParser

private data class FuriganaExpressionReading(
    val kanji: String,
    val kana: String
)

private data class FuriganaExpression(
    val elements: List<JMDictFuriganaRubyItem>
)

class FuriganaProvider {

    private val readingsMap: Map<FuriganaExpressionReading, FuriganaExpression> by lazy {
        JMdictFuriganaParser.parse(ProjectData.furiganaFile).associate {
            val reading = FuriganaExpressionReading(it.kanjiExpression, it.kanaExpression)
            reading to FuriganaExpression(it.furigana)
        }
    }

    fun getFuriganaForReading(kanjiReading: String, kanaReading: String): List<FuriganaElement>? {
        val reading = FuriganaExpressionReading(kanjiReading, kanaReading)
        return readingsMap[reading]?.elements?.map { FuriganaElement(it.ruby, it.rt) }
    }

}