package com.example.chatapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.Utils
import com.example.chatapp.model.Users
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter : RecyclerView.Adapter<UserHolder>() {
    private  var listOfUser =  listOf<Users>()
    private var listener: OnUserClickListener? = null
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.userlistitem, parent, false)
        return UserHolder(view)
    }

    override fun getItemCount(): Int {
        return listOfUser.size
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val users = listOfUser[position]
        val name =users.username!!.split("\\s".toRegex())[0]

        holder.profileName.setText(name)

        if(users.status.equals("Active now")){
            holder.statusImageView.setImageResource(R.drawable.onlinestatus)
        }
        else{
            holder.statusImageView.setImageResource(R.drawable.offlinestatus)
        }

        Glide.with(holder.itemView.context).load(users.imageUrl).into(holder.imageProfile)

        holder.itemView.setOnClickListener{
            firestore.collection("Users")
                .document(Utils.getUiLoggedIn())
                .update("chattingWith", users.userid)
            listener?.onUserSelected(position,users)
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    fun setUserList(list : List<Users>){
        this.listOfUser = list
        notifyDataSetChanged()
    }
    fun setOnUserClickListener(listener: OnUserClickListener){
        this.listener = listener
    }

}
class UserHolder(itemView : View): RecyclerView.ViewHolder(itemView){
    val profileName : TextView = itemView.findViewById(R.id.userName)
    val imageProfile : CircleImageView = itemView.findViewById(R.id.imageViewUser)
    val statusImageView : ImageView = itemView.findViewById(R.id.statusOnline)

}
interface OnUserClickListener{
    fun onUserSelected(position: Int, users: Users)
}