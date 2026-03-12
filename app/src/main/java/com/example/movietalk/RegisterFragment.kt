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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
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
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
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
                    val username = email.substringBefore("@")
                    val profileUriString = selectedProfileUri?.toString()

                    viewLifecycleOwner.lifecycleScope.launch {
                        userDao.upsertUser(
                            com.example.movietalk.data.local.UserEntity(
                                uid = user.uid,
                                email = email,
                                username = username,
                                profileImageUri = profileUriString
                            )
                        )

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