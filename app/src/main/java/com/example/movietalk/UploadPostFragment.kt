package com.example.movietalk

import kotlinx.coroutines.withContext

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.movietalk.data.local.AppDatabase
import com.example.movietalk.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await

class UploadPostFragment : Fragment(R.layout.fragment_upload_post) {
    // OMDb API setup
    private val omdbApiKey = "a64aa4bd"
    private val omdbApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.example.movietalk.data.api.OmdbApiService::class.java)
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var selectedImageUri: Uri? = null
    private lateinit var repo: PostRepository
    private var fetchedMovieTitle: String? = null

    private suspend fun uploadPostImageAndGetUrl(imageUri: Uri, uid: String, postId: String): String {
        val imageRef = storage.reference
            .child("post_images")
            .child(uid)
            .child("$postId-${System.currentTimeMillis()}.jpg")

        imageRef.putFile(imageUri).await()
        return imageRef.downloadUrl.await().toString()
    }

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
        // OMDb UI elements
        val etOmdbTitle = view.findViewById<EditText>(R.id.etOmdbTitle)
        val btnFetchOmdb = view.findViewById<Button>(R.id.btnFetchOmdb)
        val tvOmdbYear = view.findViewById<TextView>(R.id.tvOmdbYear)
        val tvOmdbGenre = view.findViewById<TextView>(R.id.tvOmdbGenre)
        val tvOmdbActors = view.findViewById<TextView>(R.id.tvOmdbActors)
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val progressFetchOmdb = view.findViewById<ProgressBar>(R.id.progressFetchOmdb)

        fun setFetchLoading(isLoading: Boolean) {
            progressFetchOmdb.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnFetchOmdb.isEnabled = !isLoading
            btnFetchOmdb.text = if (isLoading) "" else "Fetch Movie Info"
        }

        etTitle.setOnClickListener {
            val title = fetchedMovieTitle?.trim().orEmpty()
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Movie title")
                .setMessage(if (title.isNotEmpty()) title else "No movie selected")
                .setPositiveButton("OK", null)
                .show()
        }

        btnFetchOmdb.setOnClickListener {
            val omdbTitle = etOmdbTitle.text?.toString()?.trim().orEmpty()
            if (omdbTitle.isEmpty()) {
                etOmdbTitle.error = "Enter a movie title"
                return@setOnClickListener
            }
            tvOmdbYear.text = "Year: ..."
            tvOmdbGenre.text = "Genre: ..."
            tvOmdbActors.text = "Actors: ..."
            // Fetch from OMDb
            lifecycleScope.launch {
                setFetchLoading(true)
                try {
                    val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        omdbApiService.getMovieByTitle(omdbTitle, omdbApiKey).execute()
                    }
                    val movie = response.body()
                    fetchedMovieTitle = movie?.Title?.trim().orEmpty().ifBlank { null }
                    etTitle.setText(fetchedMovieTitle.orEmpty())
                    etTitle.setSelection(etTitle.text?.length ?: 0)
                    tvOmdbYear.text = "Year: ${movie?.Year ?: "Not found"}"
                    tvOmdbGenre.text = "Genre: ${movie?.Genre ?: "Not found"}"
                    tvOmdbActors.text = "Actors: ${movie?.Actors ?: "Not found"}"
                } catch (e: Exception) {
                    fetchedMovieTitle = null
                    tvOmdbYear.text = "Year: Error"
                    tvOmdbGenre.text = "Genre: Error"
                    tvOmdbActors.text = "Actors: Error"
                } finally {
                    setFetchLoading(false)
                }
            }
        }
        super.onViewCreated(view, savedInstanceState)

        val localDb = AppDatabase.getInstance(requireContext())
        repo = PostRepository(FirebaseFirestore.getInstance(), localDb.postDao())

        val ivPreview = view.findViewById<ImageView>(R.id.ivPreview)
        val btnPickImage = view.findViewById<Button>(R.id.btnPickImage)
        val progressUpload = view.findViewById<ProgressBar>(R.id.progressUpload)

        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val etText = view.findViewById<EditText>(R.id.etText)

        val btnPost = view.findViewById<Button>(R.id.btnPost)

        fun setUploadLoading(isLoading: Boolean) {
            progressUpload.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnPost.isEnabled = !isLoading
            btnPickImage.isEnabled = !isLoading
            btnFetchOmdb.isEnabled = !isLoading
            btnPost.text = if (isLoading) "" else "Create Post"
        }

        // Reset form fields and preview when fragment is viewed
        fetchedMovieTitle = null
        etTitle.setText("")
        ratingBar.rating = 0f
        selectedImageUri = null
        ivPreview.apply {
            setImageDrawable(null)
            visibility = View.GONE
        }

        btnPickImage.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        btnPost.setOnClickListener {
            val title = fetchedMovieTitle?.trim().orEmpty()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Fetch a movie first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val text = etText.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                etText.error = "Write something"
                return@setOnClickListener
            }

            val user = auth.currentUser
            val uid = user?.uid ?: run {
                Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rating = ratingBar.rating
            val id = FirebaseFirestore.getInstance().collection("posts").document().id

            setUploadLoading(true)

            // Use activity's lifecycle scope instead of fragment's for more stability
            requireActivity().lifecycleScope.launch {
                try {
                    android.util.Log.d("UploadPost", "Starting post creation...")

                    val username = try {
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .get()
                            .await()

                        userDoc.getString("username")
                            ?.takeIf { it.isNotBlank() }
                            ?: withContext(kotlinx.coroutines.Dispatchers.IO) {
                                localDb.userDao().getUser(uid)?.username
                            }
                            ?: user.email?.substringBefore("@")
                            ?: "User"
                    } catch (_: Exception) {
                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                            localDb.userDao().getUser(uid)?.username
                        } ?: user.email?.substringBefore("@") ?: "User"
                    }

                    val uploadedImageUrl = selectedImageUri?.let { imageUri ->
                        android.util.Log.d("UploadPost", "Uploading image to Firebase Storage...")
                        uploadPostImageAndGetUrl(imageUri, uid, id)
                    }.orEmpty()

                    val postObj = Post(
                        id = id,
                        title = title,
                        text = text,
                        rating = rating,
                        userId = uid, // Ensure userId is set
                        userName = username,
                        imageUrl = uploadedImageUrl,
                        createdAt = System.currentTimeMillis(),
                    )

                    android.util.Log.d("UploadPost", "Adding post...")
                    try {
                        val result = withTimeoutOrNull(15000) { // 15 second timeout
                            repo.addPost(postObj)
                        }
                        if (result == null) {
                            throw Exception("Firestore write timeout - operation took too long")
                        }
                    } catch (addException: Exception) {
                        android.util.Log.e("UploadPost", "addPost failed", addException)
                        throw addException
                    }

                    android.util.Log.d("UploadPost", "Refreshing posts...")
                    try {
                        repo.refreshPosts()
                        android.util.Log.d("UploadPost", "refreshPosts completed")
                    } catch (refreshException: Exception) {
                        android.util.Log.e("UploadPost", "refreshPosts failed", refreshException)
                        throw refreshException
                    }

                    android.util.Log.d("UploadPost", "Post uploaded successfully!")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Post uploaded!", Toast.LENGTH_SHORT)
                            .show()

                        fetchedMovieTitle = null
                        etTitle.setText("")
                        etText.setText("")
                        ratingBar.rating = 0f
                        android.util.Log.d("UploadPost", "addPost completed")
                        // Always refresh posts after upload to ensure local list is up to date
                        android.util.Log.d("UploadPost", "Refreshing posts...")
                        try {
                            repo.refreshPosts()
                            android.util.Log.d("UploadPost", "refreshPosts completed")
                        } catch (refreshException: Exception) {
                            android.util.Log.e(
                                "UploadPost",
                                "refreshPosts failed",
                                refreshException
                            )
                        }
                        ivPreview.apply {
                            setImageDrawable(null)
                            visibility = View.GONE
                        }

                        android.util.Log.d("UploadPost", "Waiting before navigation...")
                        kotlinx.coroutines.delay(1000)

                        android.util.Log.d("UploadPost", "isAdded=$isAdded, view=$view")
                        if (isAdded && view != null) {
                            try {
                                android.util.Log.d("UploadPost", "Navigating to home...")
                                findNavController().navigate(R.id.action_uploadPostFragment_to_homeFragment)
                                android.util.Log.d("UploadPost", "Navigation successful!")
                            } catch (navException: Exception) {
                                android.util.Log.e("UploadPost", "Navigation failed", navException)
                                Toast.makeText(
                                    requireContext(),
                                    "Navigation error: ${navException.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            android.util.Log.e("UploadPost", "Cannot navigate: isAdded=$isAdded")
                        }
                    }

                } catch (e: Exception) {
                    android.util.Log.e("UploadPost", "Upload failed", e)
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Upload failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } finally {
                    if (isAdded) {
                        setUploadLoading(false)
                    }
                }
            }
        }
            // Scroll to bottom to reveal hidden button after layout is drawn
            view.post {
                val scrollView = view.parent as? ScrollView
                scrollView?.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
    }
}
