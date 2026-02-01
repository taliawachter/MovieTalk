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

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        _loading.value = true
        firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
            _username.value = doc.getString("username") ?: ""
            _loading.value = false
        }.addOnFailureListener {
            _loading.value = false
        }
    }

    fun saveUsername(username: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        _loading.value = true
        val data = hashMapOf<String, Any>("username" to username)
        firestore.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { _loading.value = false; onComplete(true) }
            .addOnFailureListener { _loading.value = false; onComplete(false) }
    }
}
