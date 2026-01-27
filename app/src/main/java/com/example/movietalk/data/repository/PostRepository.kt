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
                likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                likedBy = (doc.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        }
        postDao.upsertAll(posts.map { it.toEntity() })

}

    suspend fun addPost(post: Post) {
        firestore.collection("posts").document(post.id).set(post).await()
        postDao.upsertAll(listOf(post.toEntity()))
    }

    suspend fun toggleLike(postId: String, currentUserId: String) {
        val ref = firestore.collection("posts").document(postId)

        firestore.runTransaction { trx ->
            val snap = trx.get(ref)
            val likedBy = (snap.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val likesCount = (snap.getLong("likesCount") ?: 0L).toInt()

            val alreadyLiked = likedBy.contains(currentUserId)
            if (alreadyLiked) {
                trx.update(ref, "likedBy", FieldValue.arrayRemove(currentUserId))
                trx.update(ref, "likesCount", (likesCount - 1).coerceAtLeast(0))
            } else {
                trx.update(ref, "likedBy", FieldValue.arrayUnion(currentUserId))
                trx.update(ref, "likesCount", likesCount + 1)
            }
        }.await()

        refreshPosts()
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
        createdAt = createdAt,
        likesCount = likesCount
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
        likesCount = likesCount,
        likedBy = emptyList()
    )
