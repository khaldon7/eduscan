package com.example.eduscan.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.eduscan.model.Hotspot
import com.example.eduscan.model.Lesson

@Composable
fun LessonScreen(lesson: Lesson, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<Hotspot?>(null) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    val bitmap = remember(lesson.imagePath) { BitmapFactory.decodeFile(lesson.imagePath)?.asImageBitmap() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onBack) { Text("رجوع") }
        Text(lesson.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        bitmap?.let { img ->
            Box(Modifier.fillMaxWidth().aspectRatio(img.width / img.height.toFloat())) {
                Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    lesson.hotspots.forEach { h ->
                        Box(
                            Modifier
                                .offset(x = maxWidth * h.x - 14.dp, y = maxHeight * h.y - 14.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xCCE24B4A))
                                .clickable { selected = h },
                            contentAlignment = Alignment.Center
                        ) { Text("i", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (lesson.summary.isNotBlank()) Text(lesson.summary)
        lesson.sections.forEach { s ->
            Spacer(Modifier.height(12.dp))
            Text(s.heading, style = MaterialTheme.typography.titleMedium)
            Text(s.body)
        }

        if (lesson.quiz.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("اختبر نفسك", style = MaterialTheme.typography.titleLarge)
        }
        lesson.quiz.forEachIndexed { qi, q ->
            Spacer(Modifier.height(10.dp))
            Text(q.question)
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
                Text(
                    if (given == q.answerIndex) "إجابة صحيحة" else "إجابة خاطئة",
                    color = if (given == q.answerIndex) Color(0xFF1D9E75) else Color(0xFFA32D2D)
                )
            }
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
