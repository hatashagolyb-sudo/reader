package com.example.reader

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Content(val fileName: String, val paragraphs: List<String>) : UiState
    data class Error(val message: String) : UiState
}

class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = mutableStateOf<UiState>(UiState.Idle)
    val uiState: State<UiState> get() = _state

    fun load(uri: Uri) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = try {
                withContext(Dispatchers.IO) { readFile(uri) }
            } catch (e: Exception) {
                UiState.Error("Не удалось прочитать файл: ${e.localizedMessage ?: "неизвестная ошибка"}")
            }
        }
    }

    private fun readFile(uri: Uri): UiState {
        val resolver = getApplication<Application>().contentResolver
        val name = queryName(uri)
        val lower = name.lowercase()

        val paragraphs: List<String> = when {
            lower.endsWith(".fb2") -> {
                val stream = resolver.openInputStream(uri)
                    ?: return UiState.Error("Не удалось открыть файл")
                stream.use { parseFb2(it) }
            }
            lower.endsWith(".txt") -> {
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return UiState.Error("Не удалось открыть файл")
                if (bytes.isEmpty()) return UiState.Error("Файл пустой")
                decode(bytes).lines()
            }
            else -> return UiState.Error("Неподдерживаемый формат. Выберите файл .txt или .fb2")
        }

        if (paragraphs.none { it.isNotBlank() }) {
            return UiState.Error("Файл пустой или не содержит текста")
        }
        return UiState.Content(name, paragraphs)
    }

    private fun queryName(uri: Uri): String {
        getApplication<Application>().contentResolver
            .query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) return c.getString(i)
            }
        return uri.lastPathSegment ?: "file"
    }

    private fun decode(bytes: ByteArray): String {
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            String(bytes, Charset.forName("windows-1251"))
        }
        return text.removePrefix("\uFEFF")
    }


    private fun parseFb2(input: InputStream): List<String> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null) // кодировка берётся из <?xml encoding="..."?>

        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inBody = false
        var inParagraph = false
        val paragraphTags = setOf("p", "v", "subtitle", "text-author")

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name
                    when {
                        tag == "body" -> inBody = parser.getAttributeValue(null, "name") != "notes"
                        tag == "empty-line" -> if (inBody) result.add("")
                        tag in paragraphTags -> if (inBody) {
                            sb.clear()
                            inParagraph = true
                        }
                    }
                }
                XmlPullParser.TEXT -> if (inBody && inParagraph) sb.append(parser.text)
                XmlPullParser.END_TAG -> {
                    val tag = parser.name
                    if (tag == "body") inBody = false
                    if (tag in paragraphTags && inParagraph) {
                        val text = sb.toString().trim()
                        if (text.isNotEmpty()) result.add(text)
                        inParagraph = false
                    }
                    if (tag == "section" || tag == "title") result.add("")
                }
            }
            event = parser.next()
        }
        return result
    }
}