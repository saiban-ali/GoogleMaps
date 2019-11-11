package com.xenderx.googlemapssdktest.utils

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.xenderx.googlemapssdktest.models.User

class CustomMapMarker(
    private var title: String,
    private var snippet: String,
    private var position: LatLng,
    var user: User
) : ClusterItem {

    constructor() : this("", "", LatLng(0.0, 0.0), User())

    override fun getSnippet(): String {
        return snippet
    }

    override fun getTitle(): String {
        return title
    }

    override fun getPosition(): LatLng {
        return position
    }

    fun setPosition(latLng: LatLng) {
        position = latLng
    }
}