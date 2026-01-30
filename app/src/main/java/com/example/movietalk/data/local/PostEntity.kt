package com.example.movietalk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val text: String,
    val rating: Float,
    val userId: String,
    val userName: String,
    val imageUrl: String,
    val createdAt: Long
)
