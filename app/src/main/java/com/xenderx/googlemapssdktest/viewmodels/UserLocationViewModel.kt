package com.xenderx.googlemapssdktest.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xenderx.googlemapssdktest.models.UserLocation

public class UserLocationViewModel : ViewModel() {

    private var mUserLocationLivaData: MutableLiveData<UserLocation> = MutableLiveData()
    private var mUsersLocationListLiveData: MutableLiveData<ArrayList<UserLocation>> = MutableLiveData()

    public fun setUserLocation(userLocation: UserLocation) {
        mUserLocationLivaData.value = userLocation
    }

    public fun getUserLocation(): UserLocation? = mUserLocationLivaData.value

    public fun getUserLocationLiveData(): MutableLiveData<UserLocation> = mUserLocationLivaData

    public fun setUserLocationsList(userLocationsList: ArrayList<UserLocation>) {
        mUsersLocationListLiveData.value = userLocationsList
    }

    public fun getAllUserLocations(): List<UserLocation>? = mUsersLocationListLiveData.value

    public fun getUserLocationsListLiveData(): MutableLiveData<ArrayList<UserLocation>> = mUsersLocationListLiveData
}