package com.example.movietalk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.RatingBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

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
                Glide.with(itemView.context)
                    .load(value)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(imgPost)
            }

            itemView.setOnClickListener { onPostClick(post) }
        }
    }
}