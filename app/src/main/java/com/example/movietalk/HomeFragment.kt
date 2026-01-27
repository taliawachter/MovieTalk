package com.example.movietalk

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movietalk.data.local.AppDatabase
import com.example.movietalk.data.repository.PostRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var repo: PostRepository
    private lateinit var postsAdapter: PostsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val localDb = AppDatabase.getInstance(requireContext())
        repo = PostRepository(FirebaseFirestore.getInstance(), localDb.postDao())

        val rv = view.findViewById<RecyclerView>(R.id.rvPosts)
        rv.layoutManager = LinearLayoutManager(requireContext())

        postsAdapter = PostsAdapter(
            onPostClick = { post ->
                val action = HomeFragmentDirections
                    .actionHomeFragmentToPostDetailsFragment(post.id)
                findNavController().navigate(action)
            },
            onLikeClick = { post ->
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: return@PostsAdapter

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        repo.toggleLike(post.id, uid)
                    } catch (_: Exception) { }
                }
            }
        )
        rv.adapter = postsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observePosts().collect { posts ->
                postsAdapter.submitList(posts)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repo.refreshPosts()
            } catch (_: Exception) { }
        }

        val fab = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        fab.bringToFront()
        fab.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToUploadPostFragment()
            findNavController().navigate(action)
        }
    }
}
