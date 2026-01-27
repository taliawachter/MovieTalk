package com.example.movietalk

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.movietalk.databinding.DialogEditPostBinding
import com.example.movietalk.databinding.FragmentPostDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: PostDetailsFragmentArgs by navArgs()

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var currentOwnerId: String? = null
    private var currentText: String = ""
    private var currentImageUrl: String? = null

    private var pickedImageUri: Uri? = null
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedImageUri = uri
    }

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

        binding.ownerActions.visibility = View.GONE
        binding.btnEdit.setOnClickListener { openEditDialog(postId) }
        binding.btnDelete.setOnClickListener { confirmDelete(postId) }

        loadPost(postId)
    }

    private fun loadPost(postId: String) {
        binding.progress.visibility = View.VISIBLE

        db.collection("posts").document(postId).get()
            .addOnSuccessListener { doc ->
                binding.progress.visibility = View.GONE
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Post not found", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    return@addOnSuccessListener
                }

                currentText = doc.getString("text").orEmpty()
                currentImageUrl = doc.getString("imageUrl")
                currentOwnerId = doc.getString("userId")

                binding.tvText.text = currentText
                binding.tvOwner.text = "by " + (doc.getString("userName") ?: "User")

                if (!currentImageUrl.isNullOrBlank()) {
                    Glide.with(this).load(currentImageUrl).into(binding.imgPost)
                } else {
                    binding.imgPost.setImageResource(R.drawable.backimg)
                }

                val isOwner = auth.currentUser?.uid != null &&
                        auth.currentUser?.uid == currentOwnerId

                binding.ownerActions.visibility = if (isOwner) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                binding.progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to load post", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openEditDialog(postId: String) {
        pickedImageUri = null
        val d = DialogEditPostBinding.inflate(layoutInflater)

        d.etEditText.setText(currentText)
        currentImageUrl?.let { url ->
            if (url.isNotBlank()) Glide.with(this).load(url).into(d.imgPreview)
        }

        d.btnPickImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit post")
            .setView(d.root)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newText = d.etEditText.text?.toString()?.trim().orEmpty()
                if (newText.isEmpty()) {
                    d.etEditText.error = "Text required"
                    return@setOnClickListener
                }

                binding.progress.visibility = View.VISIBLE
                val uri = pickedImageUri
                if (uri != null) {
                    uploadImageThenUpdate(postId, uri, newText) {
                        binding.progress.visibility = View.GONE
                        dialog.dismiss()
                        loadPost(postId)
                    }
                } else {
                    updatePost(postId, newText, null) {
                        binding.progress.visibility = View.GONE
                        dialog.dismiss()
                        loadPost(postId)
                    }
                }
            }
        }

        dialog.show()

        dialog.window?.decorView?.postDelayed(object : Runnable {
            override fun run() {
                if (dialog.isShowing) {
                    pickedImageUri?.let { d.imgPreview.setImageURI(it) }
                    dialog.window?.decorView?.postDelayed(this, 300)
                }
            }
        }, 300)
    }

    private fun uploadImageThenUpdate(postId: String, uri: Uri, newText: String, onDone: () -> Unit) {
        val ref = storage.reference.child("posts/$postId/${UUID.randomUUID()}.jpg")
        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                ref.downloadUrl
            }
            .addOnSuccessListener { url ->
                updatePost(postId, newText, url.toString(), onDone)
            }
            .addOnFailureListener {
                binding.progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePost(postId: String, newText: String, newImageUrl: String?, onDone: () -> Unit) {
        val updates = hashMapOf<String, Any>("text" to newText)
        if (newImageUrl != null) updates["imageUrl"] = newImageUrl

        db.collection("posts").document(postId).update(updates)
            .addOnSuccessListener { onDone() }
            .addOnFailureListener {
                binding.progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(postId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete post?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deletePost(postId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(postId: String) {
        binding.progress.visibility = View.VISIBLE
        db.collection("posts").document(postId).delete()
            .addOnSuccessListener {
                binding.progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                binding.progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
