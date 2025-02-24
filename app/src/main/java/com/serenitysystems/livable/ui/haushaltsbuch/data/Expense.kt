package com.serenitysystems.livable.ui.haushaltsbuch.data

import android.os.Parcel
import android.os.Parcelable

data class Expense(
    val id: String = "",
    val kategorie: String = "",
    val betrag: Float = 0f,
    val notiz: String = "",
    val datum: String = "",
    val istEinnahme: Boolean = false,
    var isDeleted: Boolean = false,
    val userEmail: String = "",  // Speichert die E-Mail des Users
    val userNickname: String = "" // Speichert den Nickname des Users
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readFloat(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(kategorie)
        parcel.writeFloat(betrag)
        parcel.writeString(notiz)
        parcel.writeString(datum)
        parcel.writeByte(if (istEinnahme) 1 else 0)
        parcel.writeByte(if (isDeleted) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Expense> {
        override fun createFromParcel(parcel: Parcel): Expense = Expense(parcel)
        override fun newArray(size: Int): Array<Expense?> = arrayOfNulls(size)
    }
}
