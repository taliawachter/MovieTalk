package com.example.movietalk

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.movietalk.data.local.AppDatabase
import com.example.movietalk.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EditProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appDb = AppDatabase.getInstance(app.applicationContext)

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _profileImageUrl = MutableLiveData<String?>(null)
    val profileImageUrl: LiveData<String?> = _profileImageUrl

    private val _displayName = MutableLiveData<String>("")
    val displayName: LiveData<String> = _displayName

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        _loading.value = true
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                val localUser = withContext(Dispatchers.IO) { appDb.userDao().getUser(uid) }

                _displayName.value = doc.getString("username")
                    ?: doc.getString("displayName")
                    ?: localUser?.username
                    ?: auth.currentUser?.email?.substringBefore("@")
                    ?: ""
                _profileImageUrl.value = doc.getString("photo") ?: localUser?.profileImageUri
            } finally {
                _loading.value = false
            }
        }
    }

    fun encodeImageToBase64(uri: Uri, onResult: (String?) -> Unit) {
        try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            onResult(base64String)
        } catch (e: Exception) {
            onResult(null)
        }
    }

    fun saveProfile(displayName: String, photoBase64: String?, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        val email = auth.currentUser?.email.orEmpty()
        _loading.value = true

        viewModelScope.launch {
            try {
                val data = hashMapOf<String, Any>(
                    "username" to displayName,
                    "displayName" to displayName,
                    "email" to email
                )
                if (!photoBase64.isNullOrBlank()) {
                    data["photo"] = photoBase64
                }

                firestore.collection("users")
                    .document(uid)
                    .set(data, SetOptions.merge())
                    .await()

                val postsSnapshot = firestore.collection("posts")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()

                if (!postsSnapshot.isEmpty) {
                    val batch = firestore.batch()
                    postsSnapshot.documents.forEach { doc ->
                        batch.update(doc.reference, "userName", displayName)
                    }
                    batch.commit().await()
                }

                withContext(Dispatchers.IO) {
                    appDb.userDao().upsertUser(
                        UserEntity(
                            uid = uid,
                            email = email,
                            username = displayName,
                            profileImageUri = photoBase64 ?: _profileImageUrl.value
                        )
                    )
                    appDb.postDao().updateUserNameForUser(uid, displayName)
                }

                _displayName.value = displayName
                if (!photoBase64.isNullOrBlank()) {
                    _profileImageUrl.value = photoBase64
                }
                _loading.value = false
                onComplete(true)
            } catch (_: Exception) {
                _loading.value = false
                onComplete(false)
            }
        }
    }
}
