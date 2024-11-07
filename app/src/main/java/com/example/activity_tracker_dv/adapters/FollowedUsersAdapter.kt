package com.example.activity_tracker_dv.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.activity_tracker_dv.R
import com.example.activity_tracker_dv.models.Followed

class FollowedUsersAdapter(
    private val unfollowCallback: (Followed) -> Unit,
    private val kpiCallback: (Followed) -> Unit // Callback per gestire il click sull'intero item
) : RecyclerView.Adapter<FollowedUsersAdapter.FollowedUserViewHolder>() {

    private var followedUsersList: List<Followed> = emptyList()

    fun submitList(followedUsers: List<Followed>) {
        followedUsersList = followedUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowedUserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_followed_user, parent, false)
        return FollowedUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowedUserViewHolder, position: Int) {
        val followedUser = followedUsersList[position]
        holder.bind(followedUser)
    }

    override fun getItemCount(): Int {
        return followedUsersList.size
    }

    inner class FollowedUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userEmailTextView: TextView = itemView.findViewById(R.id.text_view_user_email)
        private val unfollowButton: Button = itemView.findViewById(R.id.button_unfollow)

        fun bind(followedUser: Followed) {
            userEmailTextView.text = followedUser.followed
            unfollowButton.setOnClickListener {
                unfollowCallback(followedUser)
            }

            // Aggiungi il listener per il click sull'item
            itemView.setOnClickListener {
                kpiCallback(followedUser)
            }
        }
    }
}
