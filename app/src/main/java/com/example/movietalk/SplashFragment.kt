package com.example.movietalk

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logo = view.findViewById<TextView>(R.id.tvLogo)

        logo.scaleX = 0f
        logo.scaleY = 0f
        logo.alpha = 0f

        logo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(1200)
            .withEndAction {
                goNext()
            }
            .start()
    }
    private fun goNext() {
        val user = FirebaseAuth.getInstance().currentUser
        if (!isAdded) return

        try {
            val nav = findNavController()
            if (user != null) {
                // connected -> Home
                nav.navigate(R.id.action_splashFragment_to_homeFragment)
            } else {
                // not connected -> Login (או Register)
                nav.navigate(R.id.action_splashFragment_to_loginFragment)
            }
        } catch (e: Exception) {
            // fallback: ignore navigation failure to avoid app crash
        }
    }


}
