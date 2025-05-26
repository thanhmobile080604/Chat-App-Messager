package com.example.chatapp.mvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.chatapp.Utils
import com.example.chatapp.fragment.HomeFragmentDirections
import com.example.chatapp.model.RecentChats
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatListRepo {
    private var firestore = FirebaseFirestore.getInstance()

    fun getAllChatList(): LiveData<List<RecentChats>> {
        val mainchatList = MutableLiveData<List<RecentChats>>()

        firestore.collection("Conversations${Utils.getUiLoggedIn()}")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    return@addSnapshotListener
                }
                val chatList = mutableListOf<RecentChats>()

                snapshot?.documents?.forEach { document ->
                    val chatlistModel = document.toObject(RecentChats::class.java)
                    chatlistModel.let {
                        chatList.add(it!!)
                    }
                }


                mainchatList.value = chatList
            }
        return mainchatList
    }

}