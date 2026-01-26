package com.example.movietalk

data class Post(
    val id: String = "",
    val text: String = "",
    val userId: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val createdAt: Long = 0L
)
