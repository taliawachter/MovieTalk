package com.example.movietalk

import android.net.Uri
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
import com.example.movietalk.databinding.FragmentEditPostBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditPostFragment : Fragment() {
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private val args: EditPostFragmentArgs by navArgs()
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String? = null

    private val pickImage =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, flags)
                selectedImageUri = uri
                binding.imgPreview.setImageURI(uri)
                binding.imgPreview.visibility = View.VISIBLE
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val postId = args.postId
        if (postId.isBlank()) {
            Toast.makeText(requireContext(), "Missing postId", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        loadPost(postId)

        binding.btnPickImage.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        binding.btnSaveEdit.setOnClickListener {
            saveEdit(postId)
        }
    }

    private fun loadPost(postId: String) {
        lifecycleScope.launch {
            try {
                val doc = db.collection("posts").document(postId).get().await()
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Post not found", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    return@launch
                }
                binding.etEditTitle.setText(doc.getString("title") ?: "")
                binding.etEditText.setText(doc.getString("text") ?: "")
                binding.rbEditRating.rating = (doc.getDouble("rating") ?: 0.0).toFloat()
                currentImageUrl = doc.getString("imageUrl")

                if (!currentImageUrl.isNullOrBlank()) {
                    binding.imgPreview.visibility = View.VISIBLE
                    Glide.with(this@EditPostFragment)
                        .load(currentImageUrl)
                        .into(binding.imgPreview)
                } else {
                    binding.imgPreview.visibility = View.GONE
                }
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Failed to load post", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun setSaveLoading(isLoading: Boolean) {
        binding.progressSave.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSaveEdit.isEnabled = !isLoading
        binding.btnSaveEdit.text = if (isLoading) "" else "Save Changes"
    }

    private fun saveEdit(postId: String) {
        val newTitle = binding.etEditTitle.text?.toString()?.trim().orEmpty()
        val newText = binding.etEditText.text?.toString()?.trim().orEmpty()
        val newRating = binding.rbEditRating.rating

        val updates = hashMapOf<String, Any>(
            "title" to newTitle,
            "text" to newText,
            "rating" to newRating
        )

        selectedImageUri?.let { uri ->
            updates["imageUrl"] = uri.toString()
        }

        setSaveLoading(true)
        db.collection("posts").document(postId).update(updates)
            .addOnSuccessListener {
                setSaveLoading(false)
                Toast.makeText(requireContext(), "Post updated", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                setSaveLoading(false)
                Toast.makeText(requireContext(), "Edit failed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}