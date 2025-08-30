package com.example.algoquest.model

import android.os.Parcel
import android.os.Parcelable

data class Problem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val points: Int = 0,
    val tags: List<String> = emptyList(),
    val hints: List<String> = emptyList(),
    val answer: String = "",
    val prerequisites: List<String> = emptyList() // ✅ Added this line
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList() // ✅ Added this
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(category)
        parcel.writeInt(points)
        parcel.writeStringList(tags)
        parcel.writeStringList(hints)
        parcel.writeString(answer)
        parcel.writeStringList(prerequisites) // ✅ Added this
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Problem> {
        override fun createFromParcel(parcel: Parcel): Problem = Problem(parcel)
        override fun newArray(size: Int): Array<Problem?> = arrayOfNulls(size)
    }
}
