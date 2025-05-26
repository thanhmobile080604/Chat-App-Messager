package com.example.chatapp.adapter

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.*
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.util.Util
import com.example.chatapp.R
import com.example.chatapp.Utils
import com.example.chatapp.model.RecentChats
import com.example.chatapp.model.Users
import com.example.chatapp.mvvm.ChatAppviewModel
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Locale

class RecentChatAdapter : RecyclerView.Adapter<RecentChatsHolder>() {
    private var listOfchat = listOf<RecentChats>()
    private var listener: onRecentChatClickListener? = null
    private val firestore = FirebaseFirestore.getInstance()
    private var listener2: onRecentChatLongClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentChatsHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.recentchatlist, parent, false)
        return RecentChatsHolder(view)
    }

    override fun getItemCount(): Int {
        return listOfchat.size
    }

    override fun onBindViewHolder(holder: RecentChatsHolder, position: Int) {
        val recentChatlist = listOfchat[position]


        holder.Username.setText(recentChatlist.name)

        Glide.with(holder.itemView.context).load(recentChatlist.friendImage).into(holder.imageView)

        if(recentChatlist.archive!!){
            holder.itemView.visibility  = View.GONE
        }


        if (recentChatlist.person == "you") {
            if (recentChatlist.message!!.endsWith(".jpg")) holder.lastmessage.setText("You sent an image")
            else if (recentChatlist.message!!.endsWith(".mp4")) holder.lastmessage.setText("You sent a video")
            else if (recentChatlist.message!!.endsWith(".pdf")) holder.lastmessage.setText("You sent a file")
            else {
                val realmessage = Utils.decrypt(recentChatlist.message!!)
                val thememessage =
                    if (realmessage.length > 5) "${realmessage.substring(0, 5)}..." else realmessage
                val makelastmessage = "You: ${thememessage}"
                holder.lastmessage.setText(makelastmessage)
            }
        } else {
            if (recentChatlist.message!!.endsWith(".jpg")) holder.lastmessage.setText("Sent an image")
            else if (recentChatlist.message!!.endsWith(".mp4")) holder.lastmessage.setText("Sent a video")
            else if (recentChatlist.message!!.endsWith(".pdf")) holder.lastmessage.setText("Sent a file")
            else {
                val realmessage = Utils.decrypt(recentChatlist.message!!)
                val thememessage = if (realmessage.length > 5) {
                    realmessage.substring(0, 5) + "..."
                } else {
                    realmessage
                }
                val makelastmessage = "${thememessage}"
                holder.lastmessage.setText(makelastmessage)
            }
        }

        holder.time.text = Utils.getFormattedDate(recentChatlist.timestamp!!)

        if (recentChatlist.seen == true || recentChatlist.person == "you") {
            holder.lastmessage.setTypeface(null, Typeface.NORMAL)
            holder.Username.setTypeface(null, Typeface.NORMAL)
            holder.time.setTypeface(null, Typeface.NORMAL)
            holder.newMessage.visibility = View.GONE
        } else {
            holder.lastmessage.setTypeface(null, Typeface.BOLD)
            holder.Username.setTypeface(null, Typeface.BOLD)
            holder.time.setTypeface(null, Typeface.BOLD)
            holder.newMessage.visibility = View.VISIBLE
        }

        Utils.listenToUserStatus(recentChatlist.friendid!!) { status ->
            if (status == "Active now") {
                holder.statusOnline.setImageResource(R.drawable.onlinestatus)
            } else {
                holder.statusOnline.setImageResource(R.drawable.offlinestatus)
            }
        }


        holder.itemView.setOnClickListener {
            firestore.collection("Users")
                .document(Utils.getUiLoggedIn())
                .update("chattingWith", recentChatlist.friendid)
            listener?.onRecentChatSelected(position, recentChatlist)
        }

        holder.itemView.setOnLongClickListener {
            listener2?.onRecentChatLongSelected(position, recentChatlist)
            true
        }




    }

    fun setOnRecentChatClickListener(listener: onRecentChatClickListener) {
        this.listener = listener
    }

    fun setOnRecentChatLongClickListener(listener: onRecentChatLongClickListener) {
        this.listener2 = listener
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setRecentChatList(list: List<RecentChats>) {
        this.listOfchat = list
        notifyDataSetChanged()
    }
}

class RecentChatsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val Username = itemView.findViewById<TextView>(R.id.recentChatTextName)
    val lastmessage = itemView.findViewById<TextView>(R.id.recentChatTextLastMessage)
    val time = itemView.findViewById<TextView>(R.id.recentChatTextTime)
    val imageView: CircleImageView = itemView.findViewById(R.id.recentChatImageView)
    val newMessage = itemView.findViewById<CircleImageView>(R.id.newMessage)
    val statusOnline = itemView.findViewById<CircleImageView>(R.id.statusOnline)
}

interface onRecentChatClickListener {
    fun onRecentChatSelected(position: Int, recentChats: RecentChats)

}

interface onRecentChatLongClickListener {
    fun onRecentChatLongSelected(position: Int, recentChats: RecentChats)
}