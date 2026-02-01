package com.example.movietalk.data.repository

import com.example.movietalk.Post
import com.example.movietalk.toEntity
import com.example.movietalk.toPost
import com.example.movietalk.data.local.PostDao
import com.example.movietalk.data.local.PostEntity
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

    suspend fun addPost(post: Post) {
        android.util.Log.d("PostRepository", "addPost called with id=${post.id}")
        // Save to local database first (this always works)
        try {
            android.util.Log.d("PostRepository", "Writing to local DB...")
            postDao.upsertAll(listOf(post.toEntity()))
            android.util.Log.d("PostRepository", "Local DB write successful")
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Local DB write failed", e)
            throw e
        }

        try {
            android.util.Log.d("PostRepository", "Syncing to Firestore in background...")
            val data = hashMapOf(
                // Do NOT include "id" field, Firestore document ID is the source of truth
                "title" to post.title,
                "text" to post.text,
                "rating" to post.rating,
                "userId" to post.userId,
                "imageUrl" to post.imageUrl,
                "createdAt" to post.createdAt
            )

            firestore.collection("posts")
                .document(post.id)
                .set(data)
                // Don't await - let it happen in background
                .addOnSuccessListener {
                    android.util.Log.d("PostRepository", "Firestore sync successful")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PostRepository", "Firestore sync failed", e)
                }
        } catch (e: Exception) {
            // Firestore sync error is non-critical
            android.util.Log.e("PostRepository", "Firestore sync error", e)
        }
    }

    suspend fun refreshPosts() {
        val snapshot = firestore.collection("posts").get().await()
        val posts = snapshot.documents.map { doc ->
            Post(
                id = doc.id,
                title = doc.getString("title").orEmpty(),
                text = doc.getString("text").orEmpty(),
                rating = (doc.getDouble("rating") ?: 0.0).toFloat(),
                userId = doc.getString("userId").orEmpty(),
                imageUrl = doc.getString("imageUrl").orEmpty(),
                createdAt = doc.getLong("createdAt") ?: 0L,
            )
        }
        postDao.upsertAll(posts.map { it.toEntity() })
    }

    suspend fun deletePostById(postId: String) {
        postDao.deleteById(postId)
    }
}
