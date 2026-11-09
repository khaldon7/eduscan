package com.example.eduscan.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** استخراج النص اللاتيني (إنجليزي) على الجهاز. ML Kit لا يدعم العربية. */
suspend fun extractLatinText(bitmap: Bitmap): String {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
}
