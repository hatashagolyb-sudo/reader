package com.example.reader

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel : ViewModel() {

    var lines by mutableStateOf(emptyList<String>())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun open(resolver: ContentResolver, uri: Uri) {
        loading = true
        error = null
        lines = emptyList()
        viewModelScope.launch {
            try {
                // чтение в фоновом потоке, чтобы индикатор не зависал
                lines = withContext(Dispatchers.IO) {
                    val name = fileName(resolver, uri).lowercase()
                    if (!name.endsWith(".txt") && !name.endsWith(".fb2")) {
                        throw Exception("Неподдерживаемый формат. Выберите файл .txt или .fb2")
                    }
                    val bytes = resolver.openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?: throw Exception("Не удалось открыть файл")
                    parseFile(bytes)
                }
            } catch (e: Exception) {
                error = e.message ?: "Ошибка чтения файла"
            }
            loading = false
        }
    }

    // Имя файла берём у системы
    private fun fileName(resolver: ContentResolver, uri: Uri): String {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return uri.lastPathSegment ?: ""
    }
}