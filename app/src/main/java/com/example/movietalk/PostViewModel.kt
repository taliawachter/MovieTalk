package com.example.movietalk

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.movietalk.data.local.AppDatabase
import com.example.movietalk.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class PostViewModel(app: Application) : AndroidViewModel(app) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val appDb = AppDatabase.getInstance(app.applicationContext)
    private val repo = PostRepository(firestore, appDb.postDao())

    val postsLiveData = repo.observePosts().asLiveData()

    private val _uploadLoading = MutableLiveData(false)
    val uploadLoading: LiveData<Boolean> = _uploadLoading

    private val _saveLoading = MutableLiveData(false)
    val saveLoading: LiveData<Boolean> = _saveLoading

    fun refreshPosts() {
        viewModelScope.launch {
            try {
                repo.refreshPosts()
            } catch (e: Exception) {
                android.util.Log.e("PostViewModel", "Refresh failed", e)
            }
        }
    }

    fun createPost(
        title: String,
        text: String,
        rating: Float,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onError("Not logged in")
            return
        }

        val uid = user.uid
        val postId = firestore.collection("posts").document().id
        _uploadLoading.value = true

        viewModelScope.launch {
            try {
                val username = resolveUsername(uid, user.email)
                val uploadedImageUrl = imageUri
                    ?.let { uploadPostImageAndGetUrl(it, uid, postId) }
                    .orEmpty()

                val postObj = Post(
                    id = postId,
                    title = title,
                    text = text,
                    rating = rating,
                    userId = uid,
                    userName = username,
                    imageUrl = uploadedImageUrl,
                    createdAt = System.currentTimeMillis(),
                )

                val result = withTimeoutOrNull(15000) {
                    repo.addPost(postObj)
                }

                if (result == null) {
                    throw Exception("Firestore timeout")
                }

                repo.refreshPosts()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            } finally {
                _uploadLoading.value = false
            }
        }
    }

    fun loadPost(
        postId: String,
        onLoaded: (Post) -> Unit,
        onNotFound: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("posts").document(postId).get().await()
                if (!doc.exists()) {
                    onNotFound()
                    return@launch
                }
                onLoaded(doc.toPost())
            } catch (e: Exception) {
                android.util.Log.e("PostViewModel", "Load post failed", e)
                onError()
            }
        }
    }

    fun updatePost(
        postId: String,
        title: String,
        text: String,
        rating: Float,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        _saveLoading.value = true
        viewModelScope.launch {
            try {
                val existingDoc = firestore.collection("posts").document(postId).get().await()
                if (!existingDoc.exists()) {
                    onError()
                    return@launch
                }

                val existingPost = existingDoc.toPost()
                val uid = auth.currentUser?.uid ?: existingPost.userId

                val finalImageUrl = if (imageUri != null) {
                    uploadPostImageAndGetUrl(imageUri, uid, postId)
                } else {
                    existingPost.imageUrl
                }

                val updatedPost = existingPost.copy(
                    title = title,
                    text = text,
                    rating = rating,
                    imageUrl = finalImageUrl
                )

                repo.updatePost(updatedPost)
                repo.refreshPosts()
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("PostViewModel", "Update failed", e)
                onError()
            } finally {
                _saveLoading.value = false
            }
        }
    }

    fun deletePost(
        postId: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.deletePostById(postId)
                repo.refreshPosts()
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("PostViewModel", "Delete failed", e)
                onError()
            }
        }
    }

    private suspend fun resolveUsername(uid: String, email: String?): String {
        return try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            userDoc.getString("username")
                ?.takeIf { it.isNotBlank() }
                ?: withContext(Dispatchers.IO) {
                    appDb.userDao().getUser(uid)?.username
                }
                ?: email?.substringBefore("@")
                ?: "User"
        } catch (_: Exception) {
            withContext(Dispatchers.IO) {
                appDb.userDao().getUser(uid)?.username
            } ?: email?.substringBefore("@") ?: "User"
        }
    }

    private suspend fun uploadPostImageAndGetUrl(
        imageUri: Uri,
        uid: String,
        postId: String
    ): String {
        val imageRef = storage.reference
            .child("post_images")
            .child(uid)
            .child("$postId-${System.currentTimeMillis()}.jpg")

        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }

    private fun DocumentSnapshot.toPost(): Post {
        return Post(
            id = id,
            title = getString("title").orEmpty(),
            text = getString("text").orEmpty(),
            rating = (getDouble("rating") ?: 0.0).toFloat(),
            userId = getString("userId").orEmpty(),
            userName = getString("userName").orEmpty(),
            imageUrl = getString("imageUrl").orEmpty(),
            createdAt = getLong("createdAt") ?: 0L,
        )
    }
}