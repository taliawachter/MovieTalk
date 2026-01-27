package com.example.movietalk

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var reg: ListenerRegistration? = null

    private lateinit var postsAdapter: PostsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvPosts)
        rv.layoutManager = LinearLayoutManager(requireContext())
        postsAdapter = PostsAdapter { post ->
            val bundle = Bundle().apply { putString("postId", post.id) }
            findNavController().navigate(R.id.postDetailsFragment, bundle)
        }
        rv.adapter = postsAdapter
        listenToPosts()
    }

    private fun listenToPosts() {
        reg?.remove()
        reg = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING) // הכי חדש ראשון ✅
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener

                val posts = snap.documents.map { doc ->
                    Post(
                        id = doc.id,
                        title = doc.getString("title").orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        rating = (doc.getDouble("rating") ?: 0.0).toFloat(),
                        userId = doc.getString("userId").orEmpty(),
                        userName = doc.getString("userName").orEmpty(),
                        imageUrl = doc.getString("imageUrl").orEmpty(),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }

                postsAdapter.submitList(posts)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reg?.remove()
        reg = null
    }
}
