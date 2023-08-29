package parser

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.File

data class JMdictFuriganaItem(
    @SerializedName("text")
    val kanjiExpression: String,

    @SerializedName("reading")
    val kanaExpression: String,

    @SerializedName("furigana")
    val furigana: List<JMDictFuriganaRubyItem>
)

data class JMDictFuriganaRubyItem(
    val ruby: String,
    val rt: String? = null
)

object JMdictFuriganaParser {

    fun parse(file: File): List<JMdictFuriganaItem> {
        val json = file.readText()
        return Gson().fromJson(json, object : TypeToken<List<JMdictFuriganaItem>>() {}.type)
    }

}