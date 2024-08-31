package com.example.agrigrow

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
class UserAdapter(private var users: MutableList<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userName)
        val userEmail: TextView = itemView.findViewById(R.id.userEmail)
        val userProfilePic: ImageView = itemView.findViewById(R.id.profilepic1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.userName.text = user.name
        holder.userEmail.text = user.userType

        // Load the profile picture using Glide with improved error handling
        Glide.with(holder.itemView.context)
            .load(user.profileImageUrl)
            .placeholder(R.drawable.baseline_person_24) // Placeholder image while loading

            .into(holder.userProfilePic)
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers.toMutableList()
        Log.d("UserAdapter", "Updating with ${newUsers.size} users")
        notifyDataSetChanged()
    }
}
