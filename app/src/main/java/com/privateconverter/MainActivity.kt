package com.privateconverter

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { ConverterScreen(this) } } }
    }
}

@Composable
private fun ConverterScreen(context: Context) {
    var selected by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var outputTree by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Select files and an output folder to begin.") }
    var busy by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val filesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        selected = it
        status = if (it.isEmpty()) "No files selected." else "${it.size} file(s) selected."
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        outputTree = it
        status = if (it == null) "Output folder not selected." else "Output folder selected."
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Private Media Converter", style = MaterialTheme.typography.headlineSmall)
        Text("Offline conversion on your device. Your originals are never modified.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("HEIC / HEIF → JPG")
                Text("MOV → MP4 (compatible codecs)")
                Text("No accounts • No ads • No network permission")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { filesPicker.launch(arrayOf("image/heic", "image/heif", "video/quicktime", "application/octet-stream", "*/*")) }, enabled = !busy) {
                Text("Select files")
            }
            OutlinedButton(onClick = { folderPicker.launch(null) }, enabled = !busy) { Text("Output folder") }
        }
        Text("Selected files: ${selected.size}")
        LazyColumn(Modifier.weight(1f, fill = true), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(selected) { uri ->
                Card(Modifier.fillMaxWidth()) {
                    Text(displayName(context.contentResolver, uri), Modifier.padding(10.dp))
                }
            }
        }
        if (busy) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Processing $completed of $total")
            }
        }
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && selected.isNotEmpty() && outputTree != null,
            onClick = {
                val tree = outputTree ?: return@Button
                val batch = selected
                busy = true
                completed = 0
                total = batch.size
                scope.launch {
                    val results = withContext(Dispatchers.IO) {
                        batch.map { uri ->
                            val name = displayName(context.contentResolver, uri)
                            try {
                                val result = MediaConverter.convert(context, uri, tree, name)
                                completed += 1
                                "$name → $result"
                            } catch (e: Exception) {
                                completed += 1
                                "$name: FAILED — ${e.message ?: "Unsupported or invalid media"}"
                            }
                        }
                    }
                    busy = false
                    status = results.joinToString("\n")
                }
            }
        ) { Text("Convert selected files") }
        Spacer(Modifier.height(2.dp))
    }
}

private fun displayName(resolver: ContentResolver, uri: Uri): String {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0) ?: "selected-file"
    }
    return uri.lastPathSegment ?: "selected-file"
}
