package com.example.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.reader.ui.theme.ReaderTheme

class MainActivity : ComponentActivity() {

    private val vm: ReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReaderTheme {
                ReaderScreen(vm)
            }
        }
    }
}

@Composable
fun ReaderScreen(vm: ReaderViewModel) {
    val resolver = LocalContext.current.contentResolver

    // Системное окно выбора файла
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.open(resolver, uri)
    }

    Scaffold { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Button(onClick = { picker.launch(arrayOf("*/*")) }) {
                Text("Открыть файл")
            }
            if (vm.loading) {
                CircularProgressIndicator()
            }
            vm.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            LazyColumn {
                items(vm.lines) { line ->
                    Text(line, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}