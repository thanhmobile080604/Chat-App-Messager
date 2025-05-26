package com.example.chatapp.model

import android.os.Parcel
import android.os.Parcelable

data class Message(
    val sender: String? ="",
    val receiver: String? ="",
    val message: String? ="",
    val timestamp: String? ="",
    var isTimestampVisible: Boolean = false,
    var seen: Boolean? = false
): Parcelable{
    val id: String get() = "$sender-$receiver-$message-$timestamp"

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(sender)
        parcel.writeString(receiver)
        parcel.writeString(message)
        parcel.writeString(timestamp)
        parcel.writeByte(if (isTimestampVisible) 1 else 0)
        parcel.writeValue(seen)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Message> {
        override fun createFromParcel(parcel: Parcel): Message {
            return Message(parcel)
        }

        override fun newArray(size: Int): Array<Message?> {
            return arrayOfNulls(size)
        }
    }
}