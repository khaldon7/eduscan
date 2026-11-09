package com.example.eduscan.model

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val summary: String = "",
    val sections: List<Section> = emptyList(),
    val hotspots: List<Hotspot> = emptyList(),
    val quiz: List<QuizQuestion> = emptyList(),
    val scene: Scene? = null,
    val imagePath: String = ""
)

@Serializable
data class Section(val heading: String, val body: String)

/** x و y نسبيان بين 0 و 1 من عرض الصورة وارتفاعها. */
@Serializable
data class Hotspot(val x: Float, val y: Float, val label: String, val explanation: String)

@Serializable
data class QuizQuestion(val question: String, val options: List<String>, val answerIndex: Int)

/** مشهد ثلاثي الأبعاد يصفه الذكاء الاصطناعي لأي موضوع. */
@Serializable
data class Scene(
    val animation: String = "rotate",
    val objects: List<SceneObject> = emptyList(),
    val connections: List<SceneLink> = emptyList()
)

@Serializable
data class SceneObject(
    val type: String = "sphere",
    val pos: List<Float> = listOf(0f, 0f, 0f),
    val size: List<Float> = listOf(0.5f, 1f, 1f),
    val color: String = "#888888",
    val label: String = "",
    val explanation: String = ""
)

@Serializable
data class SceneLink(val from: Int, val to: Int)
