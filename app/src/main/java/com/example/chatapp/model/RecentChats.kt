package com.example.chatapp.model

import android.os.Parcel
import android.os.Parcelable

data class RecentChats (
    val timestamp: String? = "",
    val sender: String? = "",
    val person: String? = "",
    val friendid: String? = "",
    val name: String? = "",
    val friendImage: String? = "",
    val message: String? = "",
    val status: String? = "",
    val seen: Boolean? = false,
    val archive: Boolean? = false
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(timestamp)
        parcel.writeString(sender)
        parcel.writeString(person)
        parcel.writeString(friendid)
        parcel.writeString(name)
        parcel.writeString(friendImage)
        parcel.writeString(message)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RecentChats> {
        override fun createFromParcel(parcel: Parcel): RecentChats {
            return RecentChats(parcel)
        }

        override fun newArray(size: Int): Array<RecentChats?> {
            return arrayOfNulls(size)
        }
    }

}