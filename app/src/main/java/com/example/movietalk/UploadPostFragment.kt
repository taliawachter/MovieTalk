package com.example.movietalk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.movietalk.data.local.AppDatabase
import com.example.movietalk.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

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
                view?.findViewById<ImageView>(R.id.ivPreview)?.setImageURI(uri)
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

            viewLifecycleOwner.lifecycleScope.launch {
                try {
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

                    repo.addPost(postObj)

                    Toast.makeText(requireContext(), "Post uploaded!", Toast.LENGTH_SHORT).show()

                    etTitle.setText("")
                    etText.setText("")
                    ratingBar.rating = 0f
                    selectedImageUri = null
                    ivPreview.setImageDrawable(null)

                    val nav = findNavController()

                    val popped = nav.popBackStack(R.id.homeFragment, false)

                    if (!popped) {
                        nav.navigate(
                            R.id.homeFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(nav.graph.startDestinationId, true)
                                .build()
                        )
                    }


                } catch (e: Exception) {
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
