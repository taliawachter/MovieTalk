package com.example.movietalk

data class Post(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val rating: Float = 0f,
    val userId: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val createdAt: Long = 0L,
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList()
)
