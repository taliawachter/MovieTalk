package com.example.movietalk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadPostFragment : Fragment(R.layout.fragment_upload_post) {

    private val omdbApiKey = "a64aa4bd"

    private val omdbApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.example.movietalk.data.api.OmdbApiService::class.java)
    }

    private val viewModel: PostViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var fetchedMovieTitle: String? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, flags)

                selectedImageUri = uri
                view?.findViewById<ImageView>(R.id.ivPreview)?.apply {
                    setImageURI(uri)
                    visibility = View.VISIBLE
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etOmdbTitle = view.findViewById<EditText>(R.id.etOmdbTitle)
        val btnFetchOmdb = view.findViewById<Button>(R.id.btnFetchOmdb)
        val tvOmdbYear = view.findViewById<TextView>(R.id.tvOmdbYear)
        val tvOmdbGenre = view.findViewById<TextView>(R.id.tvOmdbGenre)
        val tvOmdbActors = view.findViewById<TextView>(R.id.tvOmdbActors)
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val progressFetchOmdb = view.findViewById<ProgressBar>(R.id.progressFetchOmdb)

        val ivPreview = view.findViewById<ImageView>(R.id.ivPreview)
        val btnPickImage = view.findViewById<Button>(R.id.btnPickImage)
        val progressUpload = view.findViewById<ProgressBar>(R.id.progressUpload)
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val etText = view.findViewById<EditText>(R.id.etText)
        val btnPost = view.findViewById<Button>(R.id.btnPost)

        fun setFetchLoading(isLoading: Boolean) {
            progressFetchOmdb.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnFetchOmdb.isEnabled = !isLoading
            btnFetchOmdb.text = if (isLoading) "" else getString(R.string.fetch_movie_info)
        }

        fun setUploadLoading(isLoading: Boolean) {
            progressUpload.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnPost.isEnabled = !isLoading
            btnPickImage.isEnabled = !isLoading
            btnFetchOmdb.isEnabled = !isLoading
            btnPost.text = if (isLoading) "" else getString(R.string.create_post)
        }

        viewModel.uploadLoading.observe(viewLifecycleOwner) { isLoading ->
            setUploadLoading(isLoading)
        }

        fetchedMovieTitle = null
        etTitle.setText("")
        ratingBar.rating = 0f
        selectedImageUri = null
        ivPreview.apply {
            setImageDrawable(null)
            visibility = View.GONE
        }

        etTitle.setOnClickListener {
            val title = fetchedMovieTitle?.trim().orEmpty()
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.movie_title_dialog_title)
                .setMessage(
                    if (title.isNotEmpty()) title else getString(R.string.no_movie_selected)
                )
                .setPositiveButton(R.string.ok, null)
                .show()
        }

        btnFetchOmdb.setOnClickListener {
            val omdbTitle = etOmdbTitle.text?.toString()?.trim().orEmpty()

            if (omdbTitle.isEmpty()) {
                etOmdbTitle.error = getString(R.string.enter_movie_title)
                return@setOnClickListener
            }

            tvOmdbYear.text = getString(R.string.year_loading)
            tvOmdbGenre.text = getString(R.string.genre_loading)
            tvOmdbActors.text = getString(R.string.actors_loading)

            lifecycleScope.launch {
                setFetchLoading(true)
                try {
                    val response = withContext(Dispatchers.IO) {
                        omdbApiService.getMovieByTitle(omdbTitle, omdbApiKey).execute()
                    }

                    val movie = response.body()
                    fetchedMovieTitle = movie?.Title?.trim().orEmpty().ifBlank { null }

                    etTitle.setText(fetchedMovieTitle.orEmpty())
                    etTitle.setSelection(etTitle.text?.length ?: 0)

                    tvOmdbYear.text = getString(
                        R.string.year_value,
                        movie?.Year ?: getString(R.string.not_found)
                    )
                    tvOmdbGenre.text = getString(
                        R.string.genre_value,
                        movie?.Genre ?: getString(R.string.not_found)
                    )
                    tvOmdbActors.text = getString(
                        R.string.actors_value,
                        movie?.Actors ?: getString(R.string.not_found)
                    )

                } catch (_: Exception) {
                    fetchedMovieTitle = null
                    tvOmdbYear.text = getString(R.string.year_error)
                    tvOmdbGenre.text = getString(R.string.genre_error)
                    tvOmdbActors.text = getString(R.string.actors_error)
                } finally {
                    setFetchLoading(false)
                }
            }
        }

        btnPickImage.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        btnPost.setOnClickListener {
            val title = fetchedMovieTitle?.trim().orEmpty()
            if (title.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.fetch_movie_first),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val text = etText.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                etText.error = getString(R.string.write_something)
                return@setOnClickListener
            }

            val rating = ratingBar.rating

            viewModel.createPost(
                title = title,
                text = text,
                rating = rating,
                imageUri = selectedImageUri,
                onSuccess = {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.post_uploaded),
                            Toast.LENGTH_SHORT
                        ).show()

                        fetchedMovieTitle = null
                        etTitle.setText("")
                        etText.setText("")
                        ratingBar.rating = 0f
                        selectedImageUri = null

                        ivPreview.apply {
                            setImageDrawable(null)
                            visibility = View.GONE
                        }

                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(1000)
                            if (isAdded) {
                                try {
                                    findNavController().navigate(R.id.action_uploadPostFragment_to_homeFragment)
                                } catch (navException: Exception) {
                                    android.util.Log.e("UploadPost", "Navigation failed", navException)
                                    Toast.makeText(
                                        requireContext(),
                                        getString(
                                            R.string.navigation_error,
                                            navException.message ?: getString(R.string.unknown_error)
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                },
                onError = { message ->
                    if (isAdded) {
                        val normalizedMessage =
                            if (message.equals("Not logged in", ignoreCase = true)) {
                                getString(R.string.not_logged_in)
                            } else if (message.equals("Firestore timeout", ignoreCase = true)) {
                                getString(R.string.firestore_timeout)
                            } else {
                                message
                            }
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.upload_failed, normalizedMessage),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        view.post {
            val scrollView = view.parent as? ScrollView
            scrollView?.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}