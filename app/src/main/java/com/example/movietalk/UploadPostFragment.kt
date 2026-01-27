package com.example.movietalk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UploadPostFragment : Fragment(R.layout.fragment_upload_post) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var selectedImageUri: Uri? = null

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

            val imageValue = selectedImageUri?.toString().orEmpty()

            val rating = ratingBar.rating  // float (0..5)

            val post = hashMapOf(
                "title" to title,
                "text" to text,
                "rating" to rating,          // ✅ חדש
                "userId" to uid,
                "userName" to username,      // ✅ לא מייל
                "imageUrl" to imageValue,    // נשאר, אבל רק מהגלריה
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("posts")
                .add(post)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Post uploaded!", Toast.LENGTH_SHORT).show()

                    etTitle.setText("")
                    etText.setText("")
                    ratingBar.rating = 0f
                    selectedImageUri = null
                    ivPreview.setImageDrawable(null)

                    findNavController().navigate(
                        R.id.homeFragment,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .build()
                    )
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Upload failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
