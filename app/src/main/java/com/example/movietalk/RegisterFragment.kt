package com.example.movietalk

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.content.Intent

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment(R.layout.fragment_register) {
    private var selectedProfileUri: android.net.Uri? = null
    // No Firebase Storage needed

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri: android.net.Uri? ->
        uri?.let {
            // Persist URI permission so we can access it later for upload
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            selectedProfileUri = it
            view?.findViewById<android.widget.ImageView>(R.id.imgProfile)?.setImageURI(it)
        }
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etUsername = view.findViewById<EditText>(R.id.etRegisterUsername)
        val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = view.findViewById<EditText>(R.id.etRegisterPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvGoLogin = view.findViewById<TextView>(R.id.tvGoLogin)

        val btnSelectProfilePic = view.findViewById<Button>(R.id.btnSelectProfilePic)
        btnSelectProfilePic.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        tvGoLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            android.util.Log.d("RegisterFragment", "Register button clicked: email=$email username=$username")

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                android.util.Log.e("RegisterFragment", "Empty fields")
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                android.util.Log.e("RegisterFragment", "Password too short")
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Registering...", Toast.LENGTH_SHORT).show()
            android.util.Log.d("RegisterFragment", "Attempting registration...")

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user;
                    if (user != null) {
                        val localDb = com.example.movietalk.data.local.AppDatabase.getInstance(requireContext())
                        val userDao = localDb.userDao()
                        val profileUriString = selectedProfileUri?.toString()
                        // Save user info and image URI to Room
                        viewLifecycleOwner.lifecycleScope.launch {
                            userDao.upsertUser(
                                com.example.movietalk.data.local.UserEntity(
                                    uid = user.uid,
                                    email = email,
                                    username = username,
                                    profileImageUri = profileUriString
                                )
                            )
                            // Save to Firestore: users/{uid} = { email, username, photo }
                            val userData = hashMapOf(
                                "email" to email,
                                "username" to username
                            )
                            if (selectedProfileUri != null) {
                                try {
                                    val inputStream = requireContext().contentResolver.openInputStream(selectedProfileUri!!)
                                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                    inputStream?.close()
                                    val outputStream = java.io.ByteArrayOutputStream()
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                                    val byteArray = outputStream.toByteArray()
                                    val base64String = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
                                    userData["photo"] = base64String
                                } catch (e: Exception) {
                                    android.util.Log.e("RegisterFragment", "Failed to encode profile image", e)
                                }
                            }
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(user.uid)
                                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
                                    android.util.Log.d("RegisterFragment", "Registration and Firestore user saved, navigating to home")
                                    val options = navOptions {
                                        popUpTo(R.id.registerFragment) { inclusive = true }
                                    }
                                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment, null, options)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Failed to save user profile: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), e.message ?: "Register failed", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("RegisterFragment", "Registration failed: ${e.message}", e)
                }
        }
    }
}
