package com.example.movietalk

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class UploadPostFragment : Fragment(R.layout.fragment_upload_post) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPick = view.findViewById<Button>(R.id.btnPickImage)
        val btnPost = view.findViewById<Button>(R.id.btnPost)
        val etText = view.findViewById<EditText>(R.id.etText)
        val status = view.findViewById<TextView>(R.id.tvStatus)

        btnPick.setOnClickListener {
            status.text = "Soon: open gallery and pick image ✅"
        }

        btnPost.setOnClickListener {
            val text = etText.text.toString().trim()
            status.text = if (text.isEmpty()) "Write something first 🙂" else "Posted (demo) ✅"
        }
    }
}
