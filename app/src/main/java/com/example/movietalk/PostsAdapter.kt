package com.example.movietalk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PostsAdapter : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    // דמו זמני – אח"כ נחליף ב-Firestore
    private val posts = listOf(
        Post(
            title = "The Shawshank Redemption",
            year = "1994",
            rating = "★★★★★",
            description = "A masterpiece of storytelling and human resilience.",
            imageRes = R.drawable.wicked
        ),
        Post(
            title = "Inception",
            year = "2010",
            rating = "★★★★☆",
            description = "A mind-bending thriller by Christopher Nolan.",
            imageRes = R.drawable.wicked
        )
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount() = posts.size

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(post: Post) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = post.title
            itemView.findViewById<TextView>(R.id.tvYear).text = post.year
            itemView.findViewById<TextView>(R.id.tvRating).text = post.rating
            itemView.findViewById<TextView>(R.id.tvDesc).text = post.description
            itemView.findViewById<ImageView>(R.id.ivPoster)
                .setImageResource(post.imageRes)
        }
    }
}
