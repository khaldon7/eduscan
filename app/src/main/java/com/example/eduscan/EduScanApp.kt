package com.example.eduscan

import android.app.Application
import androidx.room.Room
import com.example.eduscan.data.AppDatabase
import com.example.eduscan.data.LessonRepository

class EduScanApp : Application() {
    lateinit var repository: LessonRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "eduscan.db").build()
        repository = LessonRepository(this, db.lessonDao())
    }
}
