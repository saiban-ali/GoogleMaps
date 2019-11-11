package com.xenderx.googlemapssdktest.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
public data class User (
    var email: String,
    var userId: String,
    var userName: String
) : Parcelable {
    constructor() : this("", "", "")
}