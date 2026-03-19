package com.example.movietalk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.movietalk.databinding.FragmentPostDetailsBinding
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
    private val viewModel: PostViewModel by viewModels()

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
        viewModel.loadPost(
            postId = postId,
            onLoaded = { post ->
                currentText = post.text
                currentImageUrl = post.imageUrl
                currentOwnerId = post.userId
                currentTitle = post.title
                currentRating = post.rating
                val userName = post.userName.ifBlank { getString(R.string.user) }

                if (!currentImageUrl.isNullOrBlank()) {
                    binding.ivPostImage.visibility = View.VISIBLE
                    Glide.with(this).load(currentImageUrl).into(binding.ivPostImage)
                } else {
                    binding.ivPostImage.visibility = View.GONE
                }

                binding.tvPostTitle.text = currentTitle
                binding.ratingBar.rating = currentRating
                binding.tvPostText.text = currentText
                binding.tvPostAuthor.text = getString(R.string.posted_by, userName)

                val currentUserUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val isOwner = currentOwnerId.isNullOrBlank().not() && currentUserUid == currentOwnerId
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

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val movie = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            omdbApiService.getMovieByTitle(currentTitle, omdbApiKey)
                        }

                        binding.tvOmdbYear.text = getString(R.string.year_value, movie.Year ?: "-")
                        binding.tvOmdbGenre.text = getString(R.string.genre_value, movie.Genre ?: "-")
                        binding.tvOmdbActors.text = getString(R.string.actors_value, movie.Actors ?: "-")
                    } catch (_: Exception) {
                        binding.tvOmdbYear.text = getString(R.string.year_value, "-")
                        binding.tvOmdbGenre.text = getString(R.string.genre_value, "-")
                        binding.tvOmdbActors.text = getString(R.string.actors_value, "-")
                    }
                }
            },
            onNotFound = {
                Toast.makeText(requireContext(), getString(R.string.post_not_found), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            },
            onError = {
                Toast.makeText(requireContext(), getString(R.string.failed_to_load_post), Toast.LENGTH_SHORT).show()
            }
        )
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
        viewModel.deletePost(
            postId = postId,
            onSuccess = {
                Toast.makeText(requireContext(), getString(R.string.deleted), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            },
            onError = {
                Toast.makeText(requireContext(), getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
