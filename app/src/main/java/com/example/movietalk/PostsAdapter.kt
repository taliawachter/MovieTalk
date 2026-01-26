package com.example.movietalk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PostsAdapter(
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.VH>() {

    private val items = mutableListOf<Post>()

    fun submitList(newItems: List<Post>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return VH(v, onPostClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class VH(itemView: View, private val onPostClick: (Post) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(post: Post) {
            // תוודאי שאלה ה-IDs שיש לך ב-item_post.xml
            itemView.findViewById<TextView>(R.id.tvTitle).text =
                if (post.userName.isNotBlank()) post.userName else "User"

            itemView.findViewById<TextView>(R.id.tvDesc).text = post.text

            itemView.setOnClickListener { onPostClick(post) }
        }
    }
}
