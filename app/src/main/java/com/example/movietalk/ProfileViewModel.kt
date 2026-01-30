package com.example.movietalk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.movietalk.data.local.AppDatabase
import com.example.movietalk.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ProfileViewModel(private val appDb: AppDatabase) : ViewModel() {
    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    fun loadUserPosts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val repo = PostRepository(FirebaseFirestore.getInstance(), appDb.postDao())
        viewModelScope.launch {
            repo.observePosts().collect { posts ->
                _userPosts.postValue(posts.filter { it.userId == userId })
            }
        }
    }
}
