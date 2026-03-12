package com.example.movietalk

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.tasks.await

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var selectedProfileUri: android.net.Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri: android.net.Uri? ->
            uri?.let {
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
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun encodeImageToBase64(uri: android.net.Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
        val etUsername = view.findViewById<EditText>(R.id.etRegisterUsername)
        val etPassword = view.findViewById<EditText>(R.id.etRegisterPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvGoLogin = view.findViewById<TextView>(R.id.tvGoLogin)
        val pbRegisterSmall = view.findViewById<android.widget.ProgressBar>(R.id.pbRegisterSmall)

        val btnSelectProfilePic = view.findViewById<Button>(R.id.btnSelectProfilePic)

        btnSelectProfilePic.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        tvGoLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pbRegisterSmall.visibility = View.VISIBLE
            btnRegister.text = ""
            btnRegister.isEnabled = false
            btnSelectProfilePic.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user

                    if (user == null) {
                        pbRegisterSmall.visibility = View.GONE
                        btnRegister.text = "Create Account"
                        btnRegister.isEnabled = true
                        btnSelectProfilePic.isEnabled = true

                        Toast.makeText(requireContext(), "Register failed", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val localDb = com.example.movietalk.data.local.AppDatabase.getInstance(requireContext())
                    val userDao = localDb.userDao()
                    val profileUriString = selectedProfileUri?.toString()
                    val photoBase64 = selectedProfileUri?.let { encodeImageToBase64(it) }

                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            userDao.upsertUser(
                                com.example.movietalk.data.local.UserEntity(
                                    uid = user.uid,
                                    email = email,
                                    username = username,
                                    profileImageUri = profileUriString
                                )
                            )

                            val userData = hashMapOf<String, Any>(
                                "email" to email,
                                "username" to username,
                                "displayName" to username
                            )
                            if (!photoBase64.isNullOrBlank()) {
                                userData["photo"] = photoBase64
                            }

                            firestore.collection("users").document(user.uid).set(userData).await()

                            pbRegisterSmall.visibility = View.GONE
                            btnRegister.text = "Create Account"
                            btnRegister.isEnabled = true
                            btnSelectProfilePic.isEnabled = true

                            val options = navOptions {
                                popUpTo(R.id.registerFragment) { inclusive = true }
                            }
                            findNavController().navigate(
                                R.id.action_registerFragment_to_homeFragment,
                                null,
                                options
                            )
                        } catch (e: Exception) {
                            pbRegisterSmall.visibility = View.GONE
                            btnRegister.text = "Create Account"
                            btnRegister.isEnabled = true
                            btnSelectProfilePic.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                e.message ?: "Register failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    pbRegisterSmall.visibility = View.GONE
                    btnRegister.text = "Create Account"
                    btnRegister.isEnabled = true
                    btnSelectProfilePic.isEnabled = true

                    Toast.makeText(requireContext(), e.message ?: "Register failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}