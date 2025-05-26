package com.example.chatapp.mvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.chatapp.Utils
import com.example.chatapp.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class MessageRepo {

    private val firestore = FirebaseFirestore.getInstance()


    fun getMessage(friendid: String): LiveData<List<Message>> {
        val messages = MutableLiveData<List<Message>>()

        val uniqueId = listOf(Utils.getUiLoggedIn(), friendid).sorted()
        uniqueId.joinToString(separator = "")

        firestore.collection("Conversations").document(uniqueId.toString()).collection("chats")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    return@addSnapshotListener
                }

                val messagesList = mutableListOf<Message>()

                if (!snapshot!!.isEmpty) {
                    snapshot.documents.forEach { document ->
                        val messageModel = document.toObject(Message::class.java)
                        if (messageModel!!.sender.equals(Utils.getUiLoggedIn()) && messageModel.receiver.equals(
                                friendid
                            ) ||
                            messageModel.sender.equals(friendid) && messageModel.receiver.equals(
                                Utils.getUiLoggedIn()
                            )
                        ) {
                            messageModel.let {
                                messagesList.add(it!!)
                            }
                        }
                    }
                    messages.value = messagesList
                }
            }
        return messages
    }
}