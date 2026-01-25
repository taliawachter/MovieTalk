package com.example.movietalk

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

        val user = FirebaseAuth.getInstance().currentUser
        tvEmail.text = user?.email ?: "Not logged in"
        tvName.text = user?.displayName ?: "MovieTalk user"

        btnEdit.setOnClickListener {
            // אפשר בהמשך לפתוח EditProfileFragment
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            findNavController().navigate(
                R.id.action_profileFragment_to_loginFragment
            )
        }
    }
}
