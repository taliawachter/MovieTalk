package com.example.movietalk

import com.example.movietalk.data.local.PostEntity

// Extension function to convert Post to PostEntity
fun Post.toEntity(): PostEntity =
    PostEntity(
        id = id,
        title = title,
        text = text,
        rating = rating,
        userId = userId,
        userName = userName,
        imageUrl = imageUrl,
        createdAt = createdAt
    )

// Extension function to convert PostEntity to Post
fun PostEntity.toPost(): Post =
    Post(
        id = id,
        title = title,
        text = text,
        rating = rating,
        userId = userId,
        userName = userName,
        imageUrl = imageUrl,
        createdAt = createdAt
    )