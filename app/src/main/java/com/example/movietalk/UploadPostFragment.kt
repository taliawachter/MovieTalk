package com.example.movietalk

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class UploadPostFragment : Fragment(R.layout.fragment_upload_post) {

    private val auth by lazy { FirebaseAuth.getInstance() }

    private var selectedImageUri: Uri? = null
    private lateinit var repo: PostRepository

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

        val localDb = AppDatabase.getInstance(requireContext())
        repo = PostRepository(FirebaseFirestore.getInstance(), localDb.postDao())

        val ivPreview = view.findViewById<ImageView>(R.id.ivPreview)
        val btnPickImage = view.findViewById<Button>(R.id.btnPickImage)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val etText = view.findViewById<EditText>(R.id.etText)

        val btnPost = view.findViewById<Button>(R.id.btnPost)

        // Reset form fields and preview when fragment is viewed
        etTitle.setText("")
        etText.setText("")
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
            val title = etTitle.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                etTitle.error = "Write a title"
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

            val username = user.email?.substringBefore("@") ?: "User"
            val rating = ratingBar.rating
            val id = FirebaseFirestore.getInstance().collection("posts").document().id

            btnPost.isEnabled = false

            // Use activity's lifecycle scope instead of fragment's for more stability
            requireActivity().lifecycleScope.launch {
                try {
                    android.util.Log.d("UploadPost", "Starting post creation...")
                    val imageUrl = selectedImageUri?.toString().orEmpty()

                    val postObj = Post(
                        id = id,
                        title = title,
                        text = text,
                        rating = rating,
                        userId = uid,
                        userName = username,
                        imageUrl = imageUrl,
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
                        android.util.Log.d("UploadPost", "addPost completed")
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
                        Toast.makeText(requireContext(), "Post uploaded!", Toast.LENGTH_SHORT).show()

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
                                        android.util.Log.e("UploadPost", "refreshPosts failed", refreshException)
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
                                Toast.makeText(requireContext(), "Navigation error: ${navException.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            android.util.Log.e("UploadPost", "Cannot navigate: isAdded=$isAdded")
                        }
                    }

                } catch (e: Exception) {
                    btnPost.isEnabled = true
                    android.util.Log.e("UploadPost", "Upload failed", e)
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Upload failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
