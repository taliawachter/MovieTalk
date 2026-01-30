package com.example.movietalk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val username: String,
    val profileImageUri: String? = null
)
