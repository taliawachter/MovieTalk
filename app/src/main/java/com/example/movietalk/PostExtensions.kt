package com.example.movietalk

import com.example.movietalk.data.local.PostEntity

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