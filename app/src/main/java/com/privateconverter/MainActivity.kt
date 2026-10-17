package com.privateconverter

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Forest = Color(0xFF176B53)
private val Ink = Color(0xFF17231F)
private val Muted = Color(0xFF65736D)
private val Canvas = Color(0xFFF4F7F4)
private val SoftGreen = Color(0xFFE4F2EA)

class MainActivity : ComponentActivity() {
    private var incomingSharedUris by mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingSharedUris = sharedUrisFrom(intent)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Forest,
                    onPrimary = Color.White,
                    secondary = Color(0xFF397D65),
                    background = Canvas,
                    surface = Color.White,
                    onSurface = Ink
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = Canvas) {
                    ConverterScreen(this, incomingSharedUris)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingSharedUris = sharedUrisFrom(intent)
    }

    private fun sharedUrisFrom(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val result = mutableListOf<Uri>()
        if (intent.action == Intent.ACTION_SEND) {
            (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let(result::add)
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            @Suppress("DEPRECATION")
            (intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))?.let(result::addAll)
        }
        intent.data?.let(result::add)
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let(result::add)
        }
        return result.distinct()
    }
}

@Composable
private fun ConverterScreen(context: Context, sharedUris: List<Uri>) {
    var selected by remember { mutableStateOf(sharedUris) }
    var outputTree by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf(if (sharedUris.isEmpty()) "Choose media to get started." else "Received " + sharedUris.size + " photo(s). Ready to convert.") }
    var busy by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var jpegQuality by remember { mutableFloatStateOf(95f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) {
            selected = sharedUris
            status = "Received " + sharedUris.size + " shared item(s). Ready to convert."
        }
    }

    val filesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        selected = uris
        status = if (uris.isEmpty()) "No files selected." else uris.size.toString() + " file(s) selected."
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        outputTree = uri
        status = if (uri == null) "Output folder not changed." else "Output folder selected."
    }

    val canSaveToPictures = selected.isNotEmpty() && selected.all { it in sharedUris }
    val canConvert = !busy && selected.isNotEmpty() && (outputTree != null || canSaveToPictures)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = Forest, modifier = Modifier.size(58.dp)) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(31.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Private Media", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                Text("CONVERTER", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, color = Muted)
            }
            Surface(color = SoftGreen, shape = CircleShape) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    androidx.compose.material3.Icon(Icons.Filled.Lock, contentDescription = null, tint = Forest, modifier = Modifier.size(14.dp))
                    Text("OFFLINE", color = Forest, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Forest)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (sharedUris.isNotEmpty()) "Photo received. You're ready." else "Your media. Your device.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    if (sharedUris.isNotEmpty()) "Convert the shared photo to JPG and save it straight to Pictures — no download step." else "Turn HEIC photos into everyday JPGs, privately and right on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE0F0E7)
                )
                if (sharedUris.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(sharedUris.size.toString() + " item(s) shared into this app", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Text("CONVERT MEDIA", style = MaterialTheme.typography.labelMedium, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(Modifier.weight(1f), { androidx.compose.material3.Icon(Icons.Filled.Image, contentDescription = null, tint = Forest, modifier = Modifier.size(23.dp)) }, "HEIC / HEIF", "to JPG")
            FeatureCard(Modifier.weight(1f), { androidx.compose.material3.Icon(Icons.Filled.Movie, contentDescription = null, tint = Forest, modifier = Modifier.size(23.dp)) }, "MOV video", "to MP4")
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { filesPicker.launch(arrayOf("image/*", "video/quicktime", "application/octet-stream", "*/*")) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text("Choose files")
                    }
                    OutlinedButton(onClick = { folderPicker.launch(null) }, enabled = !busy) {
                        androidx.compose.material3.Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Folder")
                    }
                }
                if (selected.isEmpty()) {
                    Text("No files selected yet. You can also share a photo here from WhatsApp.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Forest, modifier = Modifier.size(19.dp))
                        Text(selected.size.toString() + " selected", color = Forest, fontWeight = FontWeight.SemiBold)
                    }
                    selected.take(5).forEach { uri ->
                        Text("•  " + displayName(context.contentResolver, uri), color = Ink, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                    if (selected.size > 5) Text("and " + (selected.size - 5) + " more…", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("JPEG quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Smaller file", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Text(jpegQuality.toInt().toString() + "%", color = Forest, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("Higher quality", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Slider(value = jpegQuality, onValueChange = { jpegQuality = it }, valueRange = 50f..100f, steps = 9, enabled = !busy)
            }
        }

        if (busy) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text("Converting " + completed + " of " + total + "…", color = Ink, fontWeight = FontWeight.Medium)
                }
                LinearProgressIndicator(progress = { if (total == 0) 0f else completed.toFloat() / total }, modifier = Modifier.fillMaxWidth())
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = canConvert,
            shape = RoundedCornerShape(16.dp),
            onClick = {
                val tree = outputTree
                val batch = selected
                busy = true
                completed = 0
                total = batch.size
                scope.launch {
                    val results = mutableListOf<String>()
                    for (uri in batch) {
                        val name = displayName(context.contentResolver, uri)
                        val result = withContext(Dispatchers.IO) {
                            try {
                                val saved = if (tree != null) MediaConverter.convert(context, uri, tree, name, jpegQuality.toInt())
                                    else MediaConverter.convertImageToPictures(context, uri, name, jpegQuality.toInt())
                                "✓ " + name + " → " + saved
                            } catch (e: Exception) {
                                "✕ " + name + " — " + (e.message ?: "Unsupported or invalid media")
                            }
                        }
                        results += result
                        completed += 1
                        status = results.joinToString("\n")
                    }
                    busy = false
                }
            }
        ) {
            Text(if (outputTree == null && canSaveToPictures) "Convert & save to Pictures" else "Convert selected files", fontWeight = FontWeight.Bold)
        }

        Text(
            if (outputTree == null && canSaveToPictures) "Saves JPGs to Pictures/Private Media Converter."
            else if (outputTree != null) "Saves converted files in your chosen folder."
            else "Choose an output folder to convert manually selected files.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(color = Color(0xFFDDE5DF))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Icon(Icons.Filled.Lock, contentDescription = null, tint = Forest, modifier = Modifier.size(18.dp))
            Text("Private by design • No accounts • No ads • No internet permission", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodySmall, color = if (status.contains("✕") || status.contains("FAILED")) Color(0xFFB3261E) else Muted)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun FeatureCard(modifier: Modifier, icon: @Composable () -> Unit, title: String, subtitle: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SoftGreen)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            icon()
            Text(title, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = Forest, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun displayName(resolver: ContentResolver, uri: Uri): String {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0) ?: "shared-file"
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "shared-file"
}
