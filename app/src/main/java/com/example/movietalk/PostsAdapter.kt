package com.example.movietalk

import android.net.Uri
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
            tvTitle.text = if (post.title.isNotBlank()) post.title else "Untitled"
            tvDesc.text = post.text
            ratingBar.rating = post.rating.toFloat()

            // Fetch username from Firestore
            tvUsername.text = "..."
            val userId = post.userId
            if (userId.isNotBlank()) {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val username = doc.getString("username") ?: "User"
                        tvUsername.text = username
                    }
                    .addOnFailureListener {
                        tvUsername.text = "User"
                    }
            } else {
                tvUsername.text = "User"
            }

            val value = post.imageUrl.trim()
            if (value.isBlank()) {
                imgPost.visibility = View.GONE
            } else {
                imgPost.visibility = View.VISIBLE

                val request = if (value.startsWith("content://") || value.startsWith("file://")) {
                    Picasso.get().load(Uri.parse(value))
                } else {
                    Picasso.get().load(value)
                }
                request.into(imgPost)
            }

            itemView.setOnClickListener { onPostClick(post) }
        }
    }
}
