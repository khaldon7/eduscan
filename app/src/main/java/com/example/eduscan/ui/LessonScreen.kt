package com.example.eduscan.ui

import android.graphics.BitmapFactory
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.eduscan.model.Hotspot
import com.example.eduscan.model.Lesson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(lesson: Lesson, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("الدرس", "ثلاثي الأبعاد", "اختبار")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> LessonBody(lesson)
                1 -> ModelTab(lesson)
                else -> QuizTab(lesson)
            }
        }
    }
}

@Composable
private fun LessonBody(lesson: Lesson) {
    var selected by remember { mutableStateOf<Hotspot?>(null) }
    val bitmap = remember(lesson.imagePath) {
        if (lesson.imagePath.isNotBlank()) BitmapFactory.decodeFile(lesson.imagePath)?.asImageBitmap() else null
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        bitmap?.let { img ->
            Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).aspectRatio(img.width / img.height.toFloat())) {
                Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    lesson.hotspots.forEach { h ->
                        Box(
                            Modifier
                                .offset(x = maxWidth * h.x - 14.dp, y = maxHeight * h.y - 14.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                .clickable { selected = h },
                            contentAlignment = Alignment.Center
                        ) { Text("i", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (lesson.summary.isNotBlank()) {
            Text(lesson.summary, style = MaterialTheme.typography.bodyLarge)
        }
        lesson.sections.forEach { s ->
            Spacer(Modifier.height(16.dp))
            Text(s.heading, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(s.body, style = MaterialTheme.typography.bodyMedium)
        }
    }

    selected?.let { h ->
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("حسناً") } },
            title = { Text(h.label) },
            text = { Text(h.explanation) }
        )
    }
}

@Composable
private fun ModelTab(lesson: Lesson) {
    val context = LocalContext.current
    val scene = lesson.scene
    var status by remember { mutableStateOf("جارٍ تحميل المشهد...") }

    // مكتبة Three.js مضمّنة داخل التطبيق من assets/three.min.js
    val threeSource = remember {
        runCatching { context.assets.open("three.min.js").bufferedReader().use { it.readText() } }
            .getOrNull()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (scene == null || scene.objects.isEmpty()) {
            Text("لم يُنشأ نموذج ثلاثي الأبعاد لهذا الدرس.", style = MaterialTheme.typography.bodyLarge)
            return@Column
        }
        if (threeSource == null) {
            Text(
                "ملف مكتبة الرسم (three.min.js) غير موجود داخل التطبيق.",
                color = MaterialTheme.colorScheme.error
            )
            return@Column
        }

        val html = remember(lesson.id) { SceneHtml.build(scene, threeSource) }

        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                status = "خطأ في الصفحة: " + message.message()
                            }
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (status.startsWith("جارٍ")) status = "تم تحميل المشهد"
                        }
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            status = if (request?.isForMainFrame == true) {
                                "تعذّر تحميل الصفحة: " + error?.description
                            } else {
                                "تعذّر تحميل جزء من الصفحة: " + error?.description
                            }
                        }
                    }
                    loadDataWithBaseURL("https://eduscan.local/", html, "text/html", "utf-8", null)
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "اسحب للتدوير، واضغط على أي عنصر لمعرفة شرحه.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuizTab(lesson: Lesson) {
    val answers = remember { mutableStateMapOf<Int, Int>() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (lesson.quiz.isEmpty()) {
            Text("لا يوجد اختبار لهذا الدرس.", style = MaterialTheme.typography.bodyLarge)
        }
        lesson.quiz.forEachIndexed { qi, q ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(q.question, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    q.options.forEachIndexed { oi, opt ->
                        Row(
                            Modifier.fillMaxWidth().clickable { answers[qi] = oi },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = answers[qi] == oi, onClick = null)
                            Text(opt)
                        }
                    }
                    answers[qi]?.let { given ->
                        Spacer(Modifier.height(4.dp))
                        val correct = given == q.answerIndex
                        Text(
                            if (correct) "إجابة صحيحة" else "إجابة خاطئة",
                            color = if (correct) Color(0xFF1D9E75) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
