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

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = view.findViewById<EditText>(R.id.etRegisterPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvGoLogin = view.findViewById<TextView>(R.id.tvGoLogin)

        tvGoLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val options = navOptions {
                        popUpTo(R.id.registerFragment) { inclusive = true }
                    }
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment, null, options)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), e.message ?: "Register failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
