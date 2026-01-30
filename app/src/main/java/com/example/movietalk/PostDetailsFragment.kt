package com.example.movietalk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.movietalk.databinding.FragmentPostDetailsBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostDetailsFragment : Fragment() {
        private var currentOwnerId: String? = null
        private var currentText: String = ""
        private var currentTitle: String = ""
        private var currentRating: Float = 0f
        private var currentImageUrl: String? = null
    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    private val args: PostDetailsFragmentArgs by navArgs()
    private val db by lazy { FirebaseFirestore.getInstance() }

    // OMDb API
    private val omdbApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.example.movietalk.data.api.OmdbApiService::class.java)
    }
    private val omdbApiKey = "a64aa4bd"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val postId = args.postId
        if (postId.isBlank()) {
            Toast.makeText(requireContext(), "Missing postId", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }
        loadPost(postId)
    }

    private fun loadPost(postId: String) {
        db.collection("posts").document(postId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), getString(R.string.post_not_found), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    return@addOnSuccessListener
                }

                currentText = doc.getString("text").orEmpty()
                currentImageUrl = doc.getString("imageUrl")
                currentOwnerId = doc.getString("userId")
                currentTitle = doc.getString("title").orEmpty()
                currentRating = (doc.getDouble("rating") ?: 0.0).toFloat()
                val userName = doc.getString("userName") ?: getString(R.string.user)

                // Image
                if (!currentImageUrl.isNullOrBlank()) {
                    binding.ivPostImage.visibility = View.VISIBLE
                    Glide.with(this).load(currentImageUrl).into(binding.ivPostImage)
                } else {
                    binding.ivPostImage.visibility = View.GONE
                }

                binding.tvPostTitle.text = currentTitle
                binding.ratingBar.rating = currentRating
                binding.tvPostText.text = currentText

                // Author
                binding.tvPostAuthor.text = getString(R.string.posted_by, userName)

                // Owner actions

                val currentUserUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val isOwner = currentOwnerId != null && currentUserUid == currentOwnerId
                android.util.Log.d("PostDetails", "currentOwnerId=$currentOwnerId, currentUserUid=$currentUserUid, isOwner=$isOwner")
                binding.btnEditPost.visibility = if (isOwner) View.VISIBLE else View.GONE
                binding.btnDeletePost.visibility = if (isOwner) View.VISIBLE else View.GONE

                binding.btnEditPost.setOnClickListener {
                    val action = PostDetailsFragmentDirections.actionPostDetailsFragmentToEditPostFragment(postId)
                    findNavController().navigate(action)
                }
                binding.btnDeletePost.setOnClickListener {
                    confirmDelete(postId)
                }

                // Fetch OMDb info
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            omdbApiService.getMovieByTitle(currentTitle, omdbApiKey).execute()
                        }
                        val movie = response.body()
                        binding.tvOmdbYear.text = getString(R.string.year_colon, movie?.Year ?: "-")
                        binding.tvOmdbGenre.text = getString(R.string.genre_colon, movie?.Genre ?: "-")
                        binding.tvOmdbActors.text = getString(R.string.actors_colon, movie?.Actors ?: "-")
                    } catch (e: Exception) {
                        binding.tvOmdbYear.text = getString(R.string.year_colon, "-")
                        binding.tvOmdbGenre.text = getString(R.string.genre_colon, "-")
                        binding.tvOmdbActors.text = getString(R.string.actors_colon, "-")
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.failed_to_load_post), Toast.LENGTH_SHORT).show()
            }
    }
    private fun confirmDelete(postId: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_post_title))
            .setMessage(getString(R.string.delete_post_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ -> deletePost(postId) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deletePost(postId: String) {
        // Delete from Firebase
        db.collection("posts").document(postId).delete()
            .addOnSuccessListener {
                // Delete from local database and refresh
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val localDb = com.example.movietalk.data.local.AppDatabase.getInstance(requireContext())
                        val repo = com.example.movietalk.data.repository.PostRepository(db, localDb.postDao())
                        repo.deletePostById(postId)
                        repo.refreshPosts()
                    } catch (e: Exception) {
                        android.util.Log.e("PostDetails", "Failed to delete local post", e)
                    }
                }
                Toast.makeText(requireContext(), getString(R.string.deleted), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp() // or navigate to Home
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
