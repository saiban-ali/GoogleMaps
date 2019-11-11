package com.xenderx.googlemapssdktest.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.xenderx.googlemapssdktest.R
import com.xenderx.googlemapssdktest.UserClient
import com.xenderx.googlemapssdktest.models.UserLocation
import kotlin.time.seconds

class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val UPDATE_INTERVAL: Long = 4 * 1000
        private const val FASTEST_INTERVAL: Long = 2 * 1000
    }

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "channel_location"
            val notificationChannel = NotificationChannel(channelId, "Location Channel", NotificationManager.IMPORTANCE_DEFAULT)

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(notificationChannel)
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("")
                .setContentText("")
                .build()

            startForeground(5001, notification)
        }

        Log.d(TAG, "onCreate: Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d(TAG, "onStartCommand: called")

        getLocation()

        return START_STICKY
    }

    private fun getLocation() {
        val locationRequest = LocationRequest().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_INTERVAL
        }

        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        mFusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    val location = locationResult?.lastLocation
                    location?.let { loc ->
                        val user = (applicationContext as UserClient).user
                        user?.let {u ->
                            val geoPoint = GeoPoint(loc.latitude, loc.longitude)
                            val userLocation = UserLocation(geoPoint, null, u)
                            saveUserLocation(userLocation)
                        }
                    }
                }
            },
            Looper.myLooper()
        )
    }

    private fun saveUserLocation(userLocation: UserLocation) {
        FirebaseAuth.getInstance().uid?.let { id ->
            FirebaseFirestore.getInstance()
                .collection(getString(R.string.collection_user_location))
                .document(id)
                .set(userLocation)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "saveUserLocation: ${userLocation.geoLocation}")
                    }
                }
            return
        }

        stopSelf()
        Log.d(TAG, "saveUserLocation: Service stopped")
    }
}
