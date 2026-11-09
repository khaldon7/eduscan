package com.example.eduscan.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val title: String,
    val json: String,
    val createdAt: Long
)

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY createdAt DESC")
    fun all(): Flow<List<LessonEntity>>

    @Upsert
    suspend fun save(entity: LessonEntity)
}

@Database(entities = [LessonEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
}
