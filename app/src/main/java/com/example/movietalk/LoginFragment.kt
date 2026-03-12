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

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etLoginEmail)
        val etPassword = view.findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val loginToSingUp = view.findViewById<TextView>(R.id.logintosingup)
        val pbLoginSmall = view.findViewById<android.widget.ProgressBar>(R.id.pbLoginSmall)

        loginToSingUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pbLoginSmall.visibility = View.VISIBLE
            btnLogin.text = ""
            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    pbLoginSmall.visibility = View.GONE
                    btnLogin.text = getString(R.string.sign_in)
                    btnLogin.isEnabled = true

                    val options = navOptions {
                        popUpTo(R.id.loginFragment) { inclusive = true }
                    }
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment, null, options)
                }
                .addOnFailureListener { e ->
                    pbLoginSmall.visibility = View.GONE
                    btnLogin.text = getString(R.string.sign_in)
                    btnLogin.isEnabled = true

                    Toast.makeText(requireContext(), e.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
