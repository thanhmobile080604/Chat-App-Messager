package com.example.chatapp.mvvm

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.MyApplication
import com.example.chatapp.SharedPrefs
import com.example.chatapp.Utils
import com.example.chatapp.model.Message
import com.example.chatapp.model.RecentChats
import com.example.chatapp.model.Users
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatAppviewModel : ViewModel() {
    val name = MutableLiveData<String>()
    val imageURL = MutableLiveData<String>()
    val message = MutableLiveData<String>()

    private val firestore = FirebaseFirestore.getInstance()

    val usersRepo = UsersRepo()
    val messageRepo = MessageRepo()
    val recentchatListRepo = ChatListRepo()


    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }


    init {
        getCurrentUser()
    }

    fun getUsers(): LiveData<List<Users>> {
        return usersRepo.getUser()
    }

    fun getCurrentUser() = viewModelScope.launch(Dispatchers.IO) {
        val context = MyApplication.instance.applicationContext
        firestore.collection("Users").document(Utils.getUiLoggedIn())
            .addSnapshotListener { value, error ->
                if (value!!.exists() && value != null) {
                    val user = value.toObject(Users::class.java)
                    name.value = user?.username!!
                    imageURL.value = user.imageUrl!!

                    val myshared = SharedPrefs(context)
                    myshared.setValue("name", user.username!!)

                }
            }
    }


    fun sendMessage(sender: String, receiver: String, friendname: String, friendImage: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val context = MyApplication.instance.applicationContext

            val convoHashMap = hashMapOf<String, Any>(
                "sender" to sender,
                "receiver" to receiver,
                "message" to Utils.encrypt(message.value!!),
                "timestamp" to Utils.getTimeStamp(),
                "seen" to false
            )

            val uniqueId = listOf(sender, receiver).sorted()
            uniqueId.joinToString(separator = "")

            val friendnamesplit = friendname.split("\\s".toRegex())[0]
            val myShared = SharedPrefs(context)
            myShared.setValue("friendid", receiver)
            myShared.setValue("chatroomid", uniqueId.toString())
            myShared.setValue("friendname", friendnamesplit)
            myShared.setValue("friendImage", friendImage)

            firestore.collection("Conversations").document(uniqueId.toString())
                .collection("chats").document(Utils.getTimeStamp())
                .set(convoHashMap).addOnCompleteListener { task ->

                    val hashMapForRecentChat = hashMapOf<String, Any>(
                        "friendid" to receiver,
                        "timestamp" to Utils.getTimeStamp(),
                        "sender" to Utils.getUiLoggedIn(),
                        "message" to Utils.encrypt(message.value!!),
                        "friendImage" to friendImage,
                        "name" to friendname,
                        "person" to "you",
                        "seen" to false,
                        "archive" to false
                    )

                    firestore.collection("Conversations${Utils.getUiLoggedIn()}").document(receiver)
                        .set(hashMapForRecentChat, SetOptions.merge())


                    val receiverRecentChatMap = hashMapOf<String, Any>(
                        "friendid" to Utils.getUiLoggedIn(),
                        "timestamp" to Utils.getTimeStamp(),
                        "sender" to Utils.getUiLoggedIn(),
                        "message" to Utils.encrypt(message.value!!),
                        "friendImage" to imageURL.value!!,
                        "name" to name.value!!,
                        "person" to name.value!!,
                        "seen" to false,
                        "archive" to false
                    )

                    firestore.collection("Conversations${receiver}").document(Utils.getUiLoggedIn())
                        .set(receiverRecentChatMap, SetOptions.merge())


                    if (task.isSuccessful) {
                        message.value = ""
                    }
                }


        }

    fun sendMedia(
        sender: String,
        receiver: String,
        friendname: String,
        friendImage: String,
        publicUrl: String
    ) =
        viewModelScope.launch(Dispatchers.IO) {
            val context = MyApplication.instance.applicationContext

            val convoHashMap = hashMapOf<String, Any>(
                "sender" to sender,
                "receiver" to receiver,
                "message" to publicUrl,
                "timestamp" to Utils.getTimeStamp(),
                "seen" to false
            )

            val uniqueId = listOf(sender, receiver).sorted()
            uniqueId.joinToString(separator = "")

            val friendnamesplit = friendname.split("\\s".toRegex())[0]
            val myShared = SharedPrefs(context)
            myShared.setValue("friendid", receiver)
            myShared.setValue("chatroomid", uniqueId.toString())
            myShared.setValue("friendname", friendnamesplit)
            myShared.setValue("friendImage", friendImage)

            firestore.collection("Conversations").document(uniqueId.toString())
                .collection("chats").document(Utils.getTimeStamp())
                .set(convoHashMap).addOnCompleteListener { task ->

                    val hashMapForRecentChat = hashMapOf<String, Any>(
                        "friendid" to receiver,
                        "timestamp" to Utils.getTimeStamp(),
                        "sender" to Utils.getUiLoggedIn(),
                        "message" to publicUrl,
                        "friendImage" to friendImage,
                        "name" to friendname,
                        "person" to "you",
                        "seen" to false
                    )

                    firestore.collection("Conversations${Utils.getUiLoggedIn()}").document(receiver)
                        .set(hashMapForRecentChat, SetOptions.merge())


                    val receiverRecentChatMap = hashMapOf<String, Any>(
                        "friendid" to Utils.getUiLoggedIn(),
                        "timestamp" to Utils.getTimeStamp(),
                        "sender" to Utils.getUiLoggedIn(),
                        "message" to publicUrl,
                        "friendImage" to imageURL.value!!,
                        "name" to name.value!!,
                        "person" to name.value!!,
                        "seen" to false
                    )

                    firestore.collection("Conversations${receiver}").document(Utils.getUiLoggedIn())
                        .set(receiverRecentChatMap, SetOptions.merge())


                }


        }

    fun seenMessage(friendid: String, callback: (Boolean, String) -> Unit) {

        val uniqueId = listOf(Utils.getUiLoggedIn(), friendid).sorted()
        uniqueId.joinToString(separator = "")

        val chatsRef = firestore.collection("Conversations")
            .document(uniqueId.toString())
            .collection("chats")

        chatsRef
            .whereEqualTo("sender", friendid)
            .whereEqualTo("seen", false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    callback(true, "Không có tin nhắn nào cần đánh dấu đã xem")
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()
                querySnapshot.documents.forEach { doc ->
                    batch.update(chatsRef.document(doc.id), "seen", true)
                }

                batch.commit()
                    .addOnSuccessListener {
                        firestore.collection("Conversations${Utils.getUiLoggedIn()}")
                            .document(friendid)
                            .update("seen", true)
                        firestore.collection("Conversations${friendid}")
                            .document(Utils.getUiLoggedIn())
                            .update("seen", true)
                    }
            }
            .addOnFailureListener { e ->
                callback(false, "Lỗi khi cập nhật tin nhắn: ${e.message}")
            }


    }

    fun getMessage(friendid: String): LiveData<List<Message>> {
        return messageRepo.getMessage(friendid)
    }

    fun getRecentChatList(): LiveData<List<RecentChats>> {
        return recentchatListRepo.getAllChatList()
    }

    fun updateProfile() = viewModelScope.launch(Dispatchers.IO) {

        val context = MyApplication.instance.applicationContext

        val hashMapUser =
            hashMapOf<String, Any>(
                "username" to name.value!!,
                "imageUrl" to imageURL.value!!
            )

        firestore.collection("Users").document(Utils.getUiLoggedIn()).update(hashMapUser)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                }
            }





        val mysharedPrefs = SharedPrefs(context)
        val friendid = mysharedPrefs.getValue("friendid")

        val hashMapUpdate = hashMapOf<String, Any>(
            "friendImage" to imageURL.value!!,
            "name" to name.value!!,
            "person" to name.value!!
        )

        firestore.collection("Conversations${friendid}").document(Utils.getUiLoggedIn())
            .update(hashMapUpdate)

        firestore.collection("Conversations${Utils.getUiLoggedIn()}").document(friendid!!)
            .update("person", "you")
    }
}

