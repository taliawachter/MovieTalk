package com.example.movietalk

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val btnEdit = view.findViewById<Button>(R.id.btnEdit)
        val ivAvatar = view.findViewById<android.widget.ImageView>(R.id.ivAvatar)

        val user = FirebaseAuth.getInstance().currentUser

        val email = user?.email
        val username = email?.substringBefore("@")

        tvName.text = username ?: "MovieTalk user"
        tvEmail.text = email ?: ""

        // Show profile photo from Room (local database)
        if (user != null) {
            val localDb = com.example.movietalk.data.local.AppDatabase.getInstance(requireContext())
            val userDao = localDb.userDao()
            viewLifecycleOwner.lifecycleScope.launch {
                val userEntity = userDao.getUser(user.uid)
                val uriString = userEntity?.profileImageUri
                if (!uriString.isNullOrBlank()) {
                    try {
                        ivAvatar.setImageURI(android.net.Uri.parse(uriString))
                    } catch (e: Exception) {
                        ivAvatar.setImageResource(R.drawable.ic_profile)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_profile)
                }
            }
        } else {
            ivAvatar.setImageResource(R.drawable.ic_profile)
        }

        btnEdit.setOnClickListener {
            // TODO: Implement profile editing
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(
                R.id.action_profileFragment_to_loginFragment
            )
        }
    }
}
