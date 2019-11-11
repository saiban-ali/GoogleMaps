package com.xenderx.googlemapssdktest.utils

import com.google.firebase.firestore.GeoPoint
import com.xenderx.googlemapssdktest.models.UserLocation

interface IFragmentCallback {

    fun checkLocationPermission(): Boolean

}