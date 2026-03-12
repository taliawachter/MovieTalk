package com.example.movietalk

import androidx.core.net.toUri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import android.widget.RatingBar

class PostsAdapter(
    private val onPostClick: (Post) -> Unit,
) : RecyclerView.Adapter<PostsAdapter.VH>() {

    private val items = mutableListOf<Post>()

    fun submitList(newItems: List<Post>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return VH(v, onPostClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class VH(
        itemView: View,
        private val onPostClick: (Post) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {

        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvDesc)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        private val imgPost: ImageView = itemView.findViewById(R.id.imgPost)
        fun bind(post: Post) {
            tvTitle.text = post.title.ifBlank { "Untitled" }
            tvDesc.text = post.text
            tvUsername.text = post.userName.ifBlank { "User" }
            ratingBar.rating = post.rating

            val value = post.imageUrl.trim()
            if (value.isBlank()) {
                imgPost.visibility = View.GONE
            } else {
                imgPost.visibility = View.VISIBLE

                val request = if (value.startsWith("content://") || value.startsWith("file://")) {
                    Picasso.get().load(value.toUri())
                } else {
                    Picasso.get().load(value)
                }
                request.into(imgPost)
            }

            itemView.setOnClickListener { onPostClick(post) }
        }
    }
}
