package com.example.reader
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

// Превращает байты файла в список строк.
fun parseFile(bytes: ByteArray): List<String> {
    if (bytes.isEmpty()) throw Exception("Файл пустой")

    val head = String(bytes.copyOf(minOf(bytes.size, 500)), Charsets.ISO_8859_1)
    val lines = if (head.contains("<FictionBook")) readFb2(bytes) else readTxt(bytes)

    if (lines.all { it.isBlank() }) throw Exception("В файле нет текста")
    return lines
}

private fun readTxt(bytes: ByteArray): List<String> {
    val utf8 = String(bytes, Charsets.UTF_8)
    // символ \uFFFD появляется, если файл не в UTF-8 (старые русские файлы в windows-1251)
    val text = if (utf8.contains('\uFFFD')) String(bytes, charset("windows-1251")) else utf8
    return text.lines()
}

private fun readFb2(bytes: ByteArray): List<String> {
    val parser = Xml.newPullParser()
    parser.setInput(ByteArrayInputStream(bytes), null)

    val result = mutableListOf<String>()
    var inBody = false
    var inParagraph = false
    var paragraph = ""

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        val tag = parser.name
        when {
            event == XmlPullParser.START_TAG && tag == "body" -> inBody = true
            event == XmlPullParser.END_TAG && tag == "body" -> inBody = false
            event == XmlPullParser.START_TAG && tag == "p" && inBody -> {
                paragraph = ""
                inParagraph = true
            }
            event == XmlPullParser.TEXT && inParagraph -> paragraph += parser.text
            event == XmlPullParser.END_TAG && tag == "p" && inParagraph -> {
                result.add(paragraph.trim())
                inParagraph = false
            }
        }
        event = parser.next()
    }
    return result
}