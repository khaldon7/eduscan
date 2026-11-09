package com.example.eduscan.model

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val summary: String = "",
    val topic: String = "none",
    val sections: List<Section> = emptyList(),
    val hotspots: List<Hotspot> = emptyList(),
    val quiz: List<QuizQuestion> = emptyList(),
    val imagePath: String = ""
)

@Serializable
data class Section(val heading: String, val body: String)

/** x و y نسبيان بين 0 و 1 من عرض الصورة وارتفاعها. */
@Serializable
data class Hotspot(val x: Float, val y: Float, val label: String, val explanation: String)

@Serializable
data class QuizQuestion(val question: String, val options: List<String>, val answerIndex: Int)
