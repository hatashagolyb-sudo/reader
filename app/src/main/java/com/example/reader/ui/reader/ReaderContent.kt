package com.example.reader.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.reader.UiState

@Composable
fun ReaderContent(state: UiState, modifier: Modifier = Modifier) {
    when (state) {
        UiState.Idle -> HintText("Выберите файл .txt или .fb2", modifier)
        UiState.Loading -> LoadingIndicator(modifier)
        is UiState.Error -> ErrorText(state.message, modifier)
        is UiState.Content -> BookView(state.fileName, state.paragraphs, modifier)
    }
}