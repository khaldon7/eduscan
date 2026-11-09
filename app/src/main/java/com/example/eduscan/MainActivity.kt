package com.example.eduscan

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.eduscan.data.LessonRepository
import com.example.eduscan.model.Lesson
import com.example.eduscan.ui.EduScanTheme
import com.example.eduscan.ui.LessonScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as EduScanApp).repository
        setContent { EduScanTheme { AppRoot(repo) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(repo: LessonRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lessons by repo.lessons().collectAsState(initial = emptyList())
    var current by remember { mutableStateOf<Lesson?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            error = null
            try {
                val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                current = repo.analyzeAndSave(bytes)
            } catch (e: Exception) {
                error = e.message ?: "حدث خطأ أثناء التحليل"
            }
            busy = false
        }
    }

    val open = current
    if (open != null) {
        LessonScreen(open, onBack = { current = null })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("EduScan", style = MaterialTheme.typography.titleLarge) })
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (busy) "جارٍ التحليل..." else "مسح صفحة") },
                    expanded = !busy
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                if (lessons.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("لا توجد دروس بعد", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "اضغط مسح صفحة لتحويل صفحة الكتاب إلى درس تفاعلي",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(lessons, key = { it.id }) { lesson ->
                            LessonCard(lesson) { current = lesson }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonCard(lesson: Lesson, onClick: () -> Unit) {
    val bitmap = remember(lesson.imagePath) {
        if (lesson.imagePath.isNotBlank()) BitmapFactory.decodeFile(lesson.imagePath)?.asImageBitmap() else null
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (lesson.summary.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        lesson.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (lesson.topic == "heart") {
                    Spacer(Modifier.height(10.dp))
                    AssistChip(onClick = {}, label = { Text("يتضمن نموذجاً ثلاثي الأبعاد") })
                }
            }
        }
    }
}
