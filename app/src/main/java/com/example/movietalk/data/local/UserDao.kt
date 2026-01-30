package com.example.movietalk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM user WHERE uid = :uid LIMIT 1")
    suspend fun getUser(uid: String): UserEntity?

    @Update
    suspend fun updateUser(user: UserEntity)
}
