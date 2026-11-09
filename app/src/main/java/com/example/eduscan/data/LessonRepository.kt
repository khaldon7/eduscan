package com.example.eduscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.eduscan.BuildConfig
import com.example.eduscan.model.Lesson
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class AnalyzeRequest(val imageBase64: String, val mimeType: String = "image/jpeg")

interface AnalyzerApi {
    @POST("analyze")
    suspend fun analyze(@Body body: AnalyzeRequest): Lesson
}

class LessonRepository(private val context: Context, private val dao: LessonDao) {
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .callTimeout(240, TimeUnit.SECONDS)
        .build()

    private val api: AnalyzerApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AnalyzerApi::class.java)

    fun lessons(): Flow<List<Lesson>> =
        dao.all().map { rows -> rows.map { json.decodeFromString<Lesson>(it.json) } }

    suspend fun analyzeAndSave(imageBytes: ByteArray): Lesson {
        val small = shrink(imageBytes)
        val base64 = Base64.encodeToString(small, Base64.NO_WRAP)
        val remote = api.analyze(AnalyzeRequest(base64))

        val file = File(context.filesDir, "images/${remote.id}.jpg").apply { parentFile?.mkdirs() }
        file.writeBytes(small)

        val lesson = remote.copy(imagePath = file.absolutePath)
        dao.save(LessonEntity(lesson.id, lesson.title, json.encodeToString(Lesson.serializer(), lesson), System.currentTimeMillis()))
        return lesson
    }

    /** تصغير الصورة لتقليل حجم الطلب وتكلفته. */
    private fun shrink(bytes: ByteArray): ByteArray {
        val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("الصورة غير صالحة")
        val scale = 1600f / maxOf(src.width, src.height)
        val bmp = if (scale < 1f)
            Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
        else src
        return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
    }
}
