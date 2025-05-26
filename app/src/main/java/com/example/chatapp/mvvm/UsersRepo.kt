package com.example.chatapp.mvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.chatapp.Utils
import com.example.chatapp.model.Users
import com.google.firebase.firestore.FirebaseFirestore

class UsersRepo {
    private var firestore = FirebaseFirestore.getInstance()

    fun getUser(): LiveData<List<Users>> {
        val users = MutableLiveData<List<Users>>()

        firestore.collection("Users").addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                return@addSnapshotListener
            }
            val usersList = mutableListOf<Users>()

            snapshot?.documents?.forEach { document ->
                val user = document.toObject(Users::class.java)
                if (user!!.userid != Utils.getUiLoggedIn()) {
                    user.let {
                        usersList.add(it)
                    }
                }
                users.value = usersList
            }
        }
        return users
    }
}