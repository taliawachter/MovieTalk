package com.example.movietalk

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EditProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _username = MutableLiveData<String>("")
    val username: LiveData<String> = _username
    private val _profileImageBase64 = MutableLiveData<String?>(null)
    val profileImageBase64: LiveData<String?> = _profileImageBase64

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        _loading.value = true
        firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
            _username.value = doc.getString("username") ?: ""
            _profileImageBase64.value = doc.getString("photo")
            _loading.value = false
        }.addOnFailureListener {
            _loading.value = false
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

    fun saveProfile(username: String, photoBase64: String?, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        _loading.value = true
        val data = hashMapOf<String, Any>("username" to username)
        if (!photoBase64.isNullOrBlank()) data["photo"] = photoBase64
        firestore.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { _loading.value = false; onComplete(true) }
            .addOnFailureListener { _loading.value = false; onComplete(false) }
    }
}
