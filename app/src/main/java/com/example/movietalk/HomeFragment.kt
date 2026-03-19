package com.example.movietalk

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: PostViewModel by viewModels()
    private lateinit var postsAdapter: PostsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvPosts)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressHome)

        rv.layoutManager = LinearLayoutManager(requireContext())

        postsAdapter = PostsAdapter(
            onPostClick = { post ->
                val action = HomeFragmentDirections
                    .actionHomeFragmentToPostDetailsFragment(post.id)
                findNavController().navigate(action)
            }
        )
        rv.adapter = postsAdapter

        viewModel.postsLiveData.observe(viewLifecycleOwner) { posts ->
            postsAdapter.submitList(posts)
            progressBar.visibility = View.GONE
        }

        progressBar.visibility = View.VISIBLE
        viewModel.refreshPosts()
    }
}