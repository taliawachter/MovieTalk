package com.example.movietalk


import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.movietalk.data.local.AppDatabase

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private val firestore = FirebaseFirestore.getInstance()
    private val viewModel: ProfileViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(requireContext())
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(db) as T
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val btnLogout = view.findViewById<android.widget.ImageButton>(R.id.btnLogout)
        val btnEdit = view.findViewById<android.widget.ImageButton>(R.id.btnEdit)
        val rvUserPosts = view.findViewById<RecyclerView>(R.id.rvUserPosts)
        rvUserPosts.layoutManager = LinearLayoutManager(requireContext())
        val postsAdapter = PostsAdapter(onPostClick = { post ->
            val action = ProfileFragmentDirections
                .actionProfileFragmentToPostDetailsFragment(post.id)
            findNavController().navigate(action)
        })
        rvUserPosts.adapter = postsAdapter

        viewModel.userPosts.observe(viewLifecycleOwner, Observer { posts ->
            postsAdapter.submitList(posts)
        })
        viewModel.loadUserPosts()
        val ivAvatar = view.findViewById<android.widget.ImageView>(R.id.ivAvatar)

        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        val uid = user?.uid
        tvEmail.text = email ?: ""

        if (uid != null) {
            firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
                val displayName = doc.getString("username")
                    ?: doc.getString("displayName")
                    ?: email?.substringBefore("@")
                    ?: "MovieTalk user"
                val photoBase64 = doc.getString("photo")
                tvName.text = displayName
                if (!photoBase64.isNullOrBlank()) {
                    try {
                        val imageBytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        ivAvatar.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        ivAvatar.setImageResource(R.drawable.ic_profile)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_profile)
                }
            }.addOnFailureListener {
                tvName.text = email?.substringBefore("@") ?: "MovieTalk user"
                ivAvatar.setImageResource(R.drawable.ic_profile)
            }
        } else {
            tvName.text = "MovieTalk user"
            ivAvatar.setImageResource(R.drawable.ic_profile)
        }

        btnEdit.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }
}