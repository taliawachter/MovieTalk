package com.example.movietalk

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UploadPostFragment : Fragment(R.layout.fragment_upload_post) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etText = view.findViewById<EditText>(R.id.etText) // ⬅️ תוודאי שזה ה-id שלך
        val btnPublish = view.findViewById<View>(R.id.btnPost)  // ⬅️ תוודאי שזה ה-id שלך

        btnPublish.setOnClickListener {
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

            val post = hashMapOf(
                "text" to text,
                "userId" to uid,
                "userName" to (user.email ?: "User"),
                "imageUrl" to "", // בינתיים בלי תמונה
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("posts")
                .add(post) // ⬅️ יוצר מסמך חדש עם Auto-ID
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Post uploaded!", Toast.LENGTH_SHORT).show()
                    etText.setText("")
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
