package com.example.movietalk

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment(R.layout.fragment_home) {

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val rv = view.findViewById<RecyclerView>(R.id.rvPosts)
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = PostsAdapter()        }
}
