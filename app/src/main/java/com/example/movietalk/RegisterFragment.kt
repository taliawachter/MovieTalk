package com.example.movietalk

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

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let {
            selectedProfileUri = it
            view?.findViewById<android.widget.ImageView>(R.id.imgProfile)?.setImageURI(it)
        }
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = view.findViewById<EditText>(R.id.etRegisterPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvGoLogin = view.findViewById<TextView>(R.id.tvGoLogin)

        val btnSelectProfilePic = view.findViewById<Button>(R.id.btnSelectProfilePic)
        btnSelectProfilePic.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        tvGoLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            android.util.Log.d("RegisterFragment", "Register button clicked: email=$email")

            if (email.isEmpty() || password.isEmpty()) {
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
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
                    android.util.Log.d("RegisterFragment", "Registration success, navigating to home")
                    val options = navOptions {
                        popUpTo(R.id.registerFragment) { inclusive = true }
                    }
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment, null, options)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), e.message ?: "Register failed", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("RegisterFragment", "Registration failed: ${e.message}", e)
                }
        }
    }
}
