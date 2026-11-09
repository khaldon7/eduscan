package com.example.eduscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.eduscan.data.LessonRepository
import com.example.eduscan.model.Lesson
import com.example.eduscan.ui.LessonScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as EduScanApp).repository
        setContent { MaterialTheme { AppRoot(repo) } }
    }
}

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
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("EduScan", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !busy
            ) { Text(if (busy) "جارٍ التحليل..." else "اختر صورة الصفحة") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(lessons) { lesson ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { current = lesson }
                    ) { Text(lesson.title, Modifier.padding(16.dp)) }
                }
            }
        }
    }
}
