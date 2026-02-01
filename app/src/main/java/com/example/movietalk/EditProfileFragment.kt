package com.example.movietalk

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.movietalk.databinding.FragmentEditProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val pickImage =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedImageUri = uri
                binding.ivEditAvatar.setImageURI(uri)
            }
        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentEditProfileBinding.bind(view)
        viewModel.loadProfile()

        viewModel.username.observe(viewLifecycleOwner, Observer {
            binding.etUsername.setText(it)
        })
        viewModel.profileImageBase64.observe(viewLifecycleOwner, Observer { base64 ->
            if (!base64.isNullOrBlank()) {
                try {
                    val imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.ivEditAvatar.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.ivEditAvatar.setImageResource(R.drawable.ic_profile)
                }
            } else {
                binding.ivEditAvatar.setImageResource(R.drawable.ic_profile)
            }
        })
        viewModel.loading.observe(viewLifecycleOwner, Observer { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSaveProfile.isEnabled = !loading
        })

        binding.btnPickImage.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        binding.btnSaveProfile.setOnClickListener {
            val username = binding.etUsername.text?.toString()?.trim().orEmpty()
            if (username.isBlank()) {
                binding.etUsername.error = "Username required"
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            if (selectedImageUri != null) {
                viewModel.encodeImageToBase64(selectedImageUri!!) { base64 ->
                    viewModel.saveProfile(username, base64) { success ->
                        binding.progressBar.visibility = View.GONE
                        if (success) {
                            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        } else {
                            Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                viewModel.saveProfile(username, null) { success ->
                    binding.progressBar.visibility = View.GONE
                    if (success) {
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
