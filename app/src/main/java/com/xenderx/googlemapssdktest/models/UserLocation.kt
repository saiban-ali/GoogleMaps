package com.xenderx.googlemapssdktest.models

import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.util.*

@Parcelize
public data class UserLocation (
    var geoLocation: @RawValue GeoPoint,
    @ServerTimestamp var timestamp: Date?,
    var user: User
) : Parcelable {
    constructor() : this(GeoPoint(0.0, 0.0), null, User())
}