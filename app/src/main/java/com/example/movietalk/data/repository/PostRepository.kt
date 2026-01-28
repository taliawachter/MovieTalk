package com.example.movietalk.data.repository

import com.example.movietalk.Post
import com.example.movietalk.data.local.PostDao
import com.example.movietalk.data.local.PostEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val firestore: FirebaseFirestore,
    private val postDao: PostDao
) {

    fun observePosts(): Flow<List<Post>> =
        postDao.observePosts().map { list -> list.map { it.toPost() } }

    suspend fun refreshPosts() {
        val snapshot = firestore.collection("posts").get().await()
        val posts = snapshot.documents.map { doc ->
            Post(
                id = doc.id,
                title = doc.getString("title").orEmpty(),
                text = doc.getString("text").orEmpty(),
                rating = (doc.getDouble("rating") ?: 0.0).toFloat(),
                userId = doc.getString("userId").orEmpty(),
                userName = doc.getString("userName").orEmpty(),
                imageUrl = doc.getString("imageUrl").orEmpty(),
                createdAt = doc.getLong("createdAt") ?: 0L,
                )
        }
        postDao.upsertAll(posts.map { it.toEntity() })

}

    suspend fun addPost(post: Post) {
        firestore.collection("posts").document(post.id).set(post).await()
        postDao.upsertAll(listOf(post.toEntity()))
    }
}

private fun Post.toEntity(): PostEntity =
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

private fun PostEntity.toPost(): Post =
    Post(
        id = id,
        title = title,
        text = text,
        rating = rating,
        userId = userId,
        userName = userName,
        imageUrl = imageUrl,
        createdAt = createdAt,
       )
